import java.nio.file.Paths

import Main._system
import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ClosedShape, FlowShape, IOResult}
import akka.stream.scaladsl.{Balance, FileIO, Flow, Framing, GraphDSL, Merge, Partition, Sink}
import akka.util.ByteString

import scala.concurrent.Future

object PaymentReader {

  case class CheckPayment(payment:String)

  def props(source:String, checkerRef:ActorRef): Props = Props(new PaymentReader(source, checkerRef))
}

class PaymentReader(source:String, checkerRef:ActorRef) extends Actor with ActorLogging {

  //println("Reader initiated!")

  var _participants:Map[String, ActorRef] = Map()

  val _sourceFile = Paths.get("src/main/resources/" + source)

  val count: Sink[String, Future[Any]] = Sink.fold[Any, String](0) {

    case v => checkerRef ! PaymentReader.CheckPayment(v._2)

    case _=> println("_")
  }

  val _flow_1:Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(ByteString("\n"), 256, allowTruncation = true)

  val foreach:Future[IOResult] = FileIO.fromPath(_sourceFile)
    .via(_flow_1.map(_.utf8String))
    .to(count).run()

  override def receive: Receive = {

    _=> println("Reader response!")
  }
}
