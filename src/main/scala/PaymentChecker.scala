import CassandraConnection.SelectParticipants
import PaymentChecker.{LoadParticipants, Ready}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.matching.Regex

object PaymentChecker {

  case class PaymentSign(sign:String)

  case class Payment(sign: PaymentChecker.PaymentSign, value: Long, participant: ActorRef)

  case class LoadParticipants()

  case class Ready()

  def props(cassandraRef:ActorRef, configuration:Configuration): Props = Props(new PaymentChecker(cassandraRef, configuration))
}

class PaymentChecker(cassandraRef:ActorRef, configuration:Configuration) extends Actor with ActorLogging {

  private var participants = Map.empty[String, ActorRef]

  private val logIncorrectPayment:ActorRef = createLogIncorrectPayment()

  private val mask:String = configuration.mask

  override def receive: Receive = {

    case LoadParticipants() => {

      implicit val timeout = Timeout(10 seconds)

      val future = cassandraRef ? SelectParticipants

      val result = Await.result(future, timeout.duration).asInstanceOf[List[(String, Float)]]

      result.foreach(i => println(i))

      result.foreach(i => participants += i._1 -> createPaymentParticipant(i._1, i._2.toLong, cassandraRef))

      sender() ! Ready
    }

    case PaymentReader.CheckPayment(i) => {

      val pattern:Regex = mask.r

      if (pattern.matches(i)) {

        val pattern(value1, value2, value3) = i

        val participant1:ActorRef = getOrCreate(value1)

        val participant2:ActorRef  = getOrCreate(value2)

        participant1 ! PaymentChecker.Payment(PaymentChecker.PaymentSign("-"), value3.toLong, participant2)
      }
      else {

        logIncorrectPayment ! PaymentReader.CheckPayment(i)
      }
    }

    case PaymentReaderKafkaJson.CheckPaymentJson(b, s, a) => {

      val participant1:ActorRef = getOrCreate(b)

      val participant2:ActorRef = getOrCreate(s)

      participant1 ! PaymentChecker.Payment(PaymentChecker.PaymentSign("-"), a, participant2)
    }

    case _=> {

      log.warning("Invalid message from:" + sender())
    }
  }

  protected def createPaymentParticipant(name:String, balance:Long, cassandraRef:ActorRef): ActorRef = {

    context.actorOf(PaymentParticipant.props(name, balance, cassandraRef))
  }

  protected def createLogIncorrectPayment(): ActorRef = {

    context.actorOf(LogIncorrectPayment.props())
  }

  private def getOrCreate(name:String):ActorRef = {

    if (!participants.contains(name)) {

      participants += name -> createPaymentParticipant(name, configuration.balance, cassandraRef)
    }

    participants(name)
  }
}
