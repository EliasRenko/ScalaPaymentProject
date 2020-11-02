import Main._system.dispatcher
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration.{Duration}
import scala.util.{Failure, Success}

object PaymentChecker {

  case class Payment(sign: String, value: Long)

  def props(): Props = Props(new PaymentChecker())
}

class PaymentChecker extends Actor with ActorLogging {

  println("Checker initiated!")

  private var children = Map.empty[String, ActorRef]

  private val _logIncorrectPayment:ActorRef = _createLogIncorrectPayment()

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      if (i(0) != Main.mask) {

        println("Checking: " + i(0))

        val _participant = _createPaymentParticipant(i(0))

        children += (i(0) -> _participant)

        implicit val _timeout:Timeout = new Timeout(Duration.create(5, "seconds"))

        (_participant ? PaymentParticipant.GetStatus).mapTo[PaymentParticipant.Status] onComplete {

          case Success(status) => _participant ! PaymentChecker.Payment(i(1), i(2).replace("\r", "" ).toLong)

          case Failure(error)  => log.error(error, "Can't get status!")
        }
      }
      else {

        _logIncorrectPayment ! PaymentReader.CheckPayment(i)
      }
    }

    case _=> {

      println("Checker invalid response!")
    }
  }

  // ** Defs.

  protected def _createPaymentParticipant(name:String): ActorRef = {

    context.actorOf(PaymentParticipant.props(name))
  }

  protected def _createLogIncorrectPayment(): ActorRef = {

    context.actorOf(LogIncorrectPayment.props())
  }
}
