import akka.actor.{Actor, ActorLogging, Props}

object PaymentParticipant {

  case object GetStatus

  case class Status()

  case class StopPayment()

  def props(name:String, balance:Long): Props = Props(new PaymentParticipant(name, balance))
}

class PaymentParticipant(name:String, balance:Long) extends Actor with ActorLogging {

  var _balance:Long = balance

  override def receive: Receive = {

    case PaymentParticipant.GetStatus => sender() ! PaymentParticipant.Status()

    case PaymentChecker.Payment("-", value, participant) => {

      val b = _balance - value

      if (b > 0) {

        _balance = b

        participant ! PaymentChecker.Payment("+", value, self)

        log.info("Balance: {}", _balance.toString)

        //println(name + " - " + _balance)
      }
      else {

        participant ! PaymentParticipant.StopPayment()
      }
    }

    case PaymentChecker.Payment("+", value, participant) => {

      _balance += value

      //println(name + " + " + _balance)
    }

    case PaymentParticipant.StopPayment() => {

      log.warning("Insufficient funds: {}", name)

      //println("Insufficient funds")
    }

    case _=> {

    }
  }
}
