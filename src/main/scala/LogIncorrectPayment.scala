import akka.actor.{Actor, ActorLogging, Props}

object LogIncorrectPayment {

  def props(): Props = Props(new LogIncorrectPayment())
}

class LogIncorrectPayment extends Actor with ActorLogging {

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      log.error("Error, invalid line: + i ")
    }

    case _=> {

      log.warning("Invalid message from:" + sender())
    }
  }
}
