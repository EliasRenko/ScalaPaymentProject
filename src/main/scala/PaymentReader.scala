import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

import Main.system
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future

object PaymentReader {

  case class CheckPayment(payment:String)

  def props(source:String, checkerRef:ActorRef): Props = Props(new PaymentReader(source, checkerRef))
}

class PaymentReader(source:String, checkerRef:ActorRef) extends Actor with ActorLogging {

  val sourceFile:Path = Paths.get(source)

  // ** val sourceFiles:List[File] = getSourceFiles(source)

  private val consumerConfig = system.settings.config.getConfig("akka.kafka.consumer")

  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
    .withBootstrapServers(Main.configuration.localHost)
    .withGroupId("consumer_group_1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  override def receive: Receive = {

    case Main.StartReading => {

      // ** Read source files.

      /*for (file <- sourceFiles) {

        val checkLines:Future[Done] = FileIO.fromPath(file.toPath).via(Framing.delimiter(ByteString("\r\n"), 256, allowTruncation = true).map(_.utf8String))
          .runForeach(i => checkerRef ! PaymentReader.CheckPayment(i))
      }*/

      // ** Read kafka topic.

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

  private def getSourceFiles(source:String):List[File] = {

    val sourceDirectory = new File(source)

    if (sourceDirectory.exists && sourceDirectory.isDirectory) {

      sourceDirectory.listFiles.filter(_.isFile).toList
    }
    else {

      List[File]()
    }
  }
}
