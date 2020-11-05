import Main._system.dispatcher
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.concurrent.duration.{Duration}
import scala.util.{Failure, Success}

object PaymentChecker {

  case class Payment(sign: String, value: Long, participant: ActorRef)

  def props(): Props = Props(new PaymentChecker())
}

class PaymentChecker extends Actor with ActorLogging {

  //println("Checker initiated!")

  private var children = Map.empty[String, ActorRef]

  private val _logIncorrectPayment:ActorRef = _createLogIncorrectPayment()

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      val _line:Array[String] = i.split(" ")

      if (_line(1) != "->" || _line(2).takeRight(1) != ":") {

        _logIncorrectPayment ! PaymentReader.CheckPayment(i)
      }

      val _name1:String = _line(0)

      _addParticipant(_name1)

      val _name2:String = _line(2).replace(":", "")

      _addParticipant(_name2)

      // **

      implicit val _timeout:Timeout = new Timeout(Duration.create(5, "seconds"))

      val _participant:ActorRef = children(_name1)

      (_participant ? PaymentParticipant.GetStatus).mapTo[PaymentParticipant.Status] onComplete {

        case Success(status) => _participant ! PaymentChecker.Payment("-", _line(3).replace("\r", "" ).toLong, children(_name2))

        case Failure(error)  => log.error(error, "Can't get status!")
      }
    }

    case _=> {

    }
  }

  // ** Defs.

  private def _addParticipant(name:String):Unit = {

      if (!children.contains(name)) {

      children += name -> _createPaymentParticipant(name, Main.balance)
    }
  }

  protected def _createPaymentParticipant(name:String, balance:Long): ActorRef = {

    context.actorOf(PaymentParticipant.props(name, balance))
  }

  protected def _createLogIncorrectPayment(): ActorRef = {

    context.actorOf(LogIncorrectPayment.props())
  }
}
