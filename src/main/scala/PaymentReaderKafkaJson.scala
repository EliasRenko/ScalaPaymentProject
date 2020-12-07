import java.nio.charset.StandardCharsets

import Main.system
import Main.system.dispatcher
import PaymentReaderKafkaJson.{CheckPaymentJson, Transaction}
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.alpakka.cassandra.{CassandraSessionSettings, CassandraWriteSettings}
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSession, CassandraSessionRegistry, CassandraSource}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object PaymentReaderKafkaJson {

  case class Transaction(name1:String, name2:String, value:Long)

  case class CheckPaymentJson(buyer: String, seller: String, value: Long)

  def props(checkerRef:ActorRef): Props = Props(new PaymentReaderKafkaJson(checkerRef))
}

class PaymentReaderKafkaJson(checkerRef:ActorRef) extends Actor with ActorLogging {

  private val consumerConfig = Main.configuration.kafkaConsumerConfig

  private val sessionSettings = CassandraSessionSettings()

  implicit val cassandraSession: CassandraSession =
    CassandraSessionRegistry.get(Main.system).sessionFor(sessionSettings)

  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(Main.configuration.kafkaHost + ":" + Main.configuration.kafkaPort)
    .withGroupId(Main.configuration.transactionsGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      val keyspace: Unit =
        CassandraSource("use test_keyspace").run().onComplete {

          case Success(s) => {

            val transactions:Future[Done] = Consumer
              .plainSource(consumerSettings, Subscriptions.topics(Main.configuration.transactionsTopic))
              .runForeach(i => checkTransaction(i.value()))
          }

          case Failure(f) => println("Cannot find keyspace: " + f.toString())
        }
    }

    case _=> log.warning("Invalid message from:" + sender())
  }

  private def checkTransaction(data:Array[Byte]):Unit = {

    val json:String = new String(data, StandardCharsets.UTF_8)

    final case class Response(transactions: List[Transaction])

    implicit val transactionFormat:JsonFormat[Transaction] = jsonFormat3(Transaction)

    implicit val responseFormat:JsonFormat[Response] = jsonFormat1(Response)

    val jsonAst =  json.parseJson.convertTo[Response]

    jsonAst.transactions.foreach(i => {

      checkerRef ! CheckPaymentJson(i.name1, i.name2, i.value)

    })

    saveRecords(jsonAst.transactions)
  }

  private def saveRecords(transactions:Seq[Transaction]):Unit = {

    val statementBinder: (Transaction, PreparedStatement) => BoundStatement =
      (transaction, preparedStatement) => preparedStatement.bind(transaction.name1, transaction.name2, Float.box(transaction.value.toFloat))

    val write: Future[Seq[Transaction]] = Source(transactions)
      .via(
        CassandraFlow.create(CassandraWriteSettings.defaults,
          "INSERT INTO transactions(name1, name2, value, id) VALUES (?, ?, ?, uuid())",
          statementBinder)
      )
      .runWith(Sink.seq)

    write.onComplete {

      case Success(s) => println(s)

      case Failure(f) => println("Cannot save record: " + f.toString())
    }
  }
}
