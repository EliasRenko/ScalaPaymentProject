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

  /** Debug **/ //private val producer:Producer = new Producer()

  /** Debug **/ private val consumer:Consumer = new Consumer()

  override def receive: Receive = {

    case PaymentReader.CheckPayment(i) => {

      //producer.createPaymentRecord(null, i)

      consumer.loadPaymentRecords("key")

      val pattern:Regex = mask.r

      if (pattern.matches(i)) {

        val pattern(value1, value2, value3) = i

        val participant1:ActorRef = getOrCreate(value1)

        val participant2:ActorRef  = getOrCreate(value2)

        participant1 ! PaymentChecker.Payment(PaymentChecker.PaymentSign("-"), value3.toLong, participant2)
      }
      else {

        logIncorrectPayment ! PaymentReader.CheckPayment(i)

        println("Incorrect mask")
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

  private def getOrCreate(name:String):ActorRef = {

    if (!participants.contains(name)) {

      participants += name -> createPaymentParticipant(name, Main.configuration.balance)
    }

    participants(name)
  }
}
