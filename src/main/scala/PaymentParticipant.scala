import akka.actor.{Actor, ActorLogging, Props}

object PaymentParticipant {

  case object GetStatus

  case class Status()

  def props(name:String): Props = Props(new PaymentParticipant(name))
}

class PaymentParticipant(name:String) extends Actor with ActorLogging {

  override def receive: Receive = {

    case PaymentParticipant.GetStatus => sender() ! PaymentParticipant.Status()

    case PaymentChecker.Payment(sign, value) => {

      var b = Main.balance

      sign match {

        case "+" => b = b + value

        case "-" => b = b - value

        case _ => throw new Exception("Invalid sign detected!")
      }

      if (b > 0) {

        Main.balance = b

        log.info("Balance: {}", Main.balance.toString)
      }
      else {

        //println("Insufficient funds!")

        log.warning("Insufficient funds: {}", name)
      }

      //println("Balance: " + Main.balance.toString)
    }

    case _=> {

      println("Participant response!")
    }
  }
}
