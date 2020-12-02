import java.nio.charset.StandardCharsets

import Main.system
import PaymentReaderKafkaJson.CheckPaymentJson
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, Reads}

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
    //.withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      val transactions:Future[Done] = Consumer
        .plainSource(consumerSettings, Subscriptions.topics(Main.configuration.transactionsTopic))
        .runForeach(i => checkTransactionJson(i.value()))
    }

    case _=> log.warning("Invalid message from:" + sender())
  }

  private def checkTransactionJson(data:Array[Byte]): Unit = {

    case class Transaction(name1: String, name2: String, value:Long)

    implicit val transactionReads: Reads[Transaction] = (
      (JsPath \ "name1").read[String]
        and (JsPath \ "name2").read[String]
        and (JsPath \ "value").read[Long]
      )(Transaction.apply _)

    val json = Json.parse(new String(data, StandardCharsets.UTF_8))

    val transactions = (json \ "transactions").as[List[Transaction]]

    transactions.foreach(i => {

      checkerRef ! CheckPaymentJson(i.name1, i.name2, i.value)
    })
  }
}
