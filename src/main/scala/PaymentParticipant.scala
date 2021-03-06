import PaymentChecker.Payment
import akka.actor.{Actor, ActorLogging, Props}

object PaymentParticipant {

  case class StopPayment()

  def props(name:String, balance:Long): Props = Props(new PaymentParticipant(name, balance))
}

class PaymentParticipant(name:String, var balance:Long) extends Actor with ActorLogging {

  override def receive: Receive = {

    case Payment(paymentSign, value, participant) => {

      paymentSign.sign match  {

        case "+" => {

          balance += value

          println(name + " + " + balance)
        }

        case "-" => {

          val b = balance - value

          if (checkBalance(value)) {

            participant ! PaymentChecker.Payment(PaymentChecker.PaymentSign("+"), value, self)

            log.info("Balance: " + balance.toString)

            println(name + " - " + balance)
          }
          else {

            log.warning("Insufficient funds at: " + name)
          }
        }

        case _=> {

          log.warning("Error, invalid payment sign!")
        }
      }
    }

    case _=> {

      log.warning("Invalid message from:" + sender())
    }
  }

  protected def checkBalance(value:Long):Boolean = {

    val finalBalance = balance - value

    if (finalBalance > 0) {

      balance = finalBalance

      return true
    }

    false
  }
}
