import akka.actor.{ActorRef, ActorSystem}

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("default")

  val configuration:Configuration = new Configuration()

  private val paymentChecker:ActorRef = createPaymentChecker()

  private val paymentReader:ActorRef = createPaymentReader(configuration.sourceFile, paymentChecker)

  paymentReader ! StartReading

  case object StartReading

  protected def createPaymentChecker(): ActorRef = {

    system.actorOf(PaymentChecker.props())
  }

  protected def createPaymentReader(source:String, checkerRef:ActorRef): ActorRef = {

    system.actorOf(PaymentReader.props(source, checkerRef))
  }
}
