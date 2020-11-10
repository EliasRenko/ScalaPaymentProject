import java.nio.file.Paths

import Main.system
import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString

import scala.concurrent.Future

object PaymentReader {

  case class CheckPayment(payment:String)

  def props(source:String, checkerRef:ActorRef): Props = Props(new PaymentReader(source, checkerRef))
}

class PaymentReader(source:String, checkerRef:ActorRef) extends Actor with ActorLogging {

  val sourceFile = Paths.get(source)

  override def receive: Receive = {

    case Main.StartReading => {

      val checkLines:Future[Done] = FileIO.fromPath(sourceFile).via(Framing.delimiter(ByteString("\r\n"), 256, allowTruncation = true).map(_.utf8String))
        .runForeach(i => checkerRef ! PaymentReader.CheckPayment(i))
    }

    case _=> log.warning("Invalid message from:" + sender())
  }
}
