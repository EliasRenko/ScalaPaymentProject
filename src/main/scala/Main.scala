
import akka.actor.{ActorRef, ActorSystem}

object Main extends App {

  implicit val _system: ActorSystem = ActorSystem("default")

  private val _configuration:Configuration = new Configuration()

  var balance:Long = _configuration.getBalance()

  private val _paymentChecker:ActorRef = _createPaymentChecker()

  private val _paymentReader:ActorRef = _createPaymentReader(_configuration.getData(), _paymentChecker)

  // ** Defs.

  protected def _createPaymentChecker(): ActorRef = {

    _system.actorOf(PaymentChecker.props())
  }

  protected def _createPaymentReader(source:String, checkerRef:ActorRef): ActorRef = {

    _system.actorOf(PaymentReader.props(source, checkerRef))
  }
}
