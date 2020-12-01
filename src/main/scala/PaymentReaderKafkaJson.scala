import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

import Main.system
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import play.api.libs.json.Json

import scala.concurrent.Future

object PaymentReaderKafkaJson {

  case class CheckPayment(payment:String)

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
        .runForeach(i => checkTransactionJson(i.value()))
    }

    case _=> log.warning("Invalid message from:" + sender())
  }

  private def checkTransactionJson(data:Array[Byte]): Unit = {

    val json = Json.parse(new String(data, StandardCharsets.UTF_8))

    val transactions = (json \ "transactions").as[List[Map[String, String]]]

    transactions.map(_("transaction")).foreach(i => checkerRef ! PaymentReader.CheckPayment(i))
  }
}
