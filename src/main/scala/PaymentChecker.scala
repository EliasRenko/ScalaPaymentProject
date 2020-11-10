import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.util.matching.Regex

object PaymentChecker {

  case class PaymentSign(sign:String)

  case class Payment(sign: PaymentChecker.PaymentSign, value: Long, participant: ActorRef)

  def props(): Props = Props(new PaymentChecker())
}

class PaymentChecker extends Actor with ActorLogging {

  private var participants = Map.empty[String, ActorRef]

  private val logIncorrectPayment:ActorRef = createLogIncorrectPayment()

  private val mask:String = Main.configuration.mask

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      val pattern:Regex = mask.r

      if (pattern.matches(i)) {

        val pattern(value1, value2, value3) = i

        getOrCreate(value1)

        getOrCreate(value2)

        val participant:ActorRef = participants(value1)

        participant ! PaymentChecker.Payment(PaymentChecker.PaymentSign("-"), value3.toLong, participants(value2))
      }
      else {

        logIncorrectPayment ! PaymentReader.CheckPayment(i)
      }
    }

    case _=> {

      log.warning("Invalid message from:" + sender())
    }
  }

  protected def createPaymentParticipant(name:String, balance:Long): ActorRef = {

    context.actorOf(PaymentParticipant.props(name, balance))
  }

  protected def createLogIncorrectPayment(): ActorRef = {

    context.actorOf(LogIncorrectPayment.props())
  }

  private def getOrCreate(name:String):Unit = {

    if (!participants.contains(name)) {

      participants += name -> createPaymentParticipant(name, Main.balance)
    }
  }
}
