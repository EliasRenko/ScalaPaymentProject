import java.io.File

import Main.system
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString

import scala.concurrent.Future

object PaymentReaderSource {

  case class CheckPayment(payment:String)

  def props(source:String, checkerRef:ActorRef): Props = Props(new PaymentReader(source, checkerRef))
}

class PaymentReaderSource(source:String, checkerRef:ActorRef) extends Actor with ActorLogging {

  val sourceFiles:List[File] = getSourceFiles(source)

  override def receive: Receive = {

    case Main.StartReading => {

      for (file <- sourceFiles) {

        val transactions:Future[Done] = FileIO.fromPath(file.toPath).via(Framing.delimiter(ByteString("\r\n"), 256, allowTruncation = true).map(_.utf8String))
          .runForeach(i => checkerRef ! PaymentReader.CheckPayment(i))
      }
    }

    case _=> log.warning("Invalid message from:" + sender())
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
