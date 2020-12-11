import java.nio.charset.StandardCharsets

import Main.system
import Main.system.dispatcher
import PaymentReaderKafkaJson.{CheckPaymentJson, Transaction}
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object PaymentReaderKafkaJson {

  case class Transaction(name1:String, name2:String, value:Long)

  case class CheckPaymentJson(buyer: String, seller: String, value: Long)

  def props(checkerRef:ActorRef, configuration:Configuration): Props = Props(new PaymentReaderKafkaJson(checkerRef, configuration))
}

class PaymentReaderKafkaJson(checkerRef:ActorRef, configuration:Configuration) extends Actor with ActorLogging {

  private val consumerConfig = Main.configuration.kafkaConsumerConfig

  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(configuration.kafkaHost + ":" + configuration.kafkaPort)
    .withGroupId(configuration.transactionsGroup)
    //.withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      implicit val timeout = Timeout(10 seconds)

      val future:Future[Any] = checkerRef ? PaymentChecker.LoadParticipants

      future.onComplete {

        case Success(result) => {

          val transactions:Future[Done] = Consumer
            .plainSource(consumerSettings, Subscriptions.topics(configuration.transactionsTopic))
            .runForeach(i => checkTransaction(i.value()))
        }

        case Failure(failure) => {

          log.error("Cannot load existing participants: " + failure)
        }
      }
    }

    case _=> log.warning("Invalid message from: " + sender())
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
  }
}
