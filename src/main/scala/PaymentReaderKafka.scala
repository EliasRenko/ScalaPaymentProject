import java.nio.charset.StandardCharsets

import Main.system
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future

object PaymentReaderKafka {

  case class CheckPayment(payment:String)

  def props(checkerRef:ActorRef): Props = Props(new PaymentReaderKafka(checkerRef))
}

class PaymentReaderKafka(checkerRef:ActorRef) extends Actor with ActorLogging {

  private val consumerConfig = Main.configuration.kafkaConsumerConfig

  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(Main.configuration.kafkaHost + ":" + Main.configuration.kafkaPort)
    .withGroupId(Main.configuration.transactionsGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      val transactions:Future[Done] = Consumer
        .plainSource(consumerSettings, Subscriptions.topics("transactions"))
        .runForeach(i => checkTransaction(i.value()))
    }

    case _=> log.warning("Invalid message from:" + sender())
  }

  private def checkTransaction(data:Array[Byte]): Unit = {

    val line:String = new String(data, StandardCharsets.UTF_8)

    checkerRef ! PaymentReader.CheckPayment(line)
  }
}
