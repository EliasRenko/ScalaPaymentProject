import java.nio.file.Paths

import Main._system
import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ClosedShape, FlowShape, IOResult}
import akka.stream.scaladsl.{Balance, FileIO, Flow, Framing, GraphDSL, Merge, Partition, Sink}
import akka.util.ByteString

import scala.concurrent.Future

object PaymentReader {

  case class CheckPayment(payment:Array[String])

  def props(source:String, checkerRef:ActorRef): Props = Props(new PaymentReader(source, checkerRef))
}

class PaymentReader(source:String, checkerRef:ActorRef) extends Actor with ActorLogging {

  println("Reader initiated!")

  val _sourceFile = Paths.get("src/main/resources/" + source)

  val count: Sink[Array[String], Future[Any]] = Sink.fold[Any, Array[String]](0) {

    case v => checkerRef ! PaymentReader.CheckPayment(v._2)

    case _=> println("_")
  }

  val _flow_1:Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString("\n"), 256, allowTruncation = true)

  val foreach:Future[IOResult] = FileIO.fromPath(_sourceFile)
    .via(_flow_1.map(_.utf8String.split(" ")))
    .to(count).run()

  override def receive: Receive = {

    _=> println("Reader response!")
  }
}
