import java.io.File

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.FileIO

import scala.concurrent.Future

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("default")

  val configuration:Configuration = new Configuration()

  private val cassandraConnection:ActorRef = createCassandraConnection()

  private val paymentChecker:ActorRef = createPaymentChecker(cassandraConnection, configuration)

  private val paymentReader:ActorRef = createPaymentReaderKafkaJson(paymentChecker, configuration)

  paymentReader ! StartReading

  case object StartReading

  protected def createPaymentChecker(cassandraRef:ActorRef, configuration:Configuration): ActorRef = {

    system.actorOf(PaymentChecker.props(cassandraRef, configuration))
  }

  protected def createPaymentReaderKafkaJson(checkerRef:ActorRef, configuration:Configuration): ActorRef = {

    system.actorOf(PaymentReaderKafkaJson.props(checkerRef, configuration))
  }

  protected def createCassandraConnection(): ActorRef = {

    system.actorOf(CassandraConnection.props())
  }
}
