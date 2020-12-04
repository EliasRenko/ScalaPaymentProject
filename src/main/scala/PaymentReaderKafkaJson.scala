import java.nio.charset.StandardCharsets

import Main.system
import PaymentReaderKafkaJson.CheckPaymentJson
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future

object PaymentReaderKafkaJson {

  case class CheckPaymentJson(buyer: String, seller: String, value: Long)

  def props(checkerRef:ActorRef): Props = Props(new PaymentReaderKafkaJson(checkerRef))
}

class PaymentReaderKafkaJson(checkerRef:ActorRef) extends Actor with ActorLogging {

  private val consumerConfig = Main.configuration.kafkaConsumerConfig

  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(Main.configuration.kafkaHost + ":" + Main.configuration.kafkaPort)
    .withGroupId(Main.configuration.transactionsGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      val transactions:Future[Done] = Consumer
        .plainSource(consumerSettings, Subscriptions.topics(Main.configuration.transactionsTopic))
        .runForeach(i => checkTransaction(i.value()))
    }

    case _=> log.warning("Invalid message from:" + sender())
  }

  private def checkTransaction(data:Array[Byte]):Unit = {

    val json:String = new String(data, StandardCharsets.UTF_8)

    final case class Transaction(name1:String, name2:String, value:Long)

    final case class Response(transactions: List[Transaction])

    implicit val transactionFormat:JsonFormat[Transaction] = jsonFormat3(Transaction)

    implicit val responseFormat:JsonFormat[Response] = jsonFormat1(Response)

    val jsonAst =  json.parseJson.convertTo[Response]

    jsonAst.transactions.foreach(i => {

      checkerRef ! CheckPaymentJson(i.name1, i.name2, i.value)
    })
  }
}
