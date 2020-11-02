import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object LogIncorrectPayment {

  def props(): Props = Props(new LogIncorrectPayment())
}

class LogIncorrectPayment extends Actor with ActorLogging {

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      log.warning("Invalid name: {}", i(0))
    }
  }

  // ** Defs.

  protected def _createPaymentParticipant(): ActorRef = {

    context.actorOf(LogIncorrectPayment.props())
  }
}
