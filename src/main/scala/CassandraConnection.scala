import CassandraConnection.{InsertParticipant, SelectParticipants}
import Main.system
import Main.system.dispatcher
import akka.Done
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSession, CassandraSessionRegistry, CassandraSource}
import akka.stream.alpakka.cassandra.{CassandraSessionSettings, CassandraWriteSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

object CassandraConnection {

  case class CheckKeyspace()

  case class Init()

  case class InsertParticipant(name:String, value:Long)

  case class SelectParticipants()

  def props(): Props = Props(new CassandraConnection())
}

class CassandraConnection() extends Actor with ActorLogging {

  implicit val cassandraSession: CassandraSession = CassandraSessionRegistry.get(Main.system).sessionFor(CassandraSessionSettings())

  override def receive: Receive = {

    case SelectParticipants => {

      val checkerSender = sender()

      var p = new ListBuffer[(String, Float)]()

      val participants: Future[Done] = CassandraSource("SELECT * FROM test_keyspace.participants")
        .map(row => (row.getString("name"), row.getFloat("value")))
        .runForeach(i => {

          p += i
        })

      participants.onComplete {

        case Success(result) => {

          checkerSender ! p.toList
        }

        case Failure(failure) => {

          log.error("Cannot load existing participants: " + failure)
        }
      }
    }

    case InsertParticipant(name, value) => {

      insertParticipantRecord(name, value)
    }

    case _ => {

    }
  }

  protected def insertParticipantRecord(name:String, value:Long):Unit = {

    case class Participant(name:String, value:Long)

    val statementBinder: (Participant, PreparedStatement) => BoundStatement =
      (transaction, preparedStatement) => preparedStatement.bind(transaction.name, Float.box(transaction.value.toFloat))

    val write: Future[Seq[Participant]] = Source.single(new Participant(name, value))
      .via(
        CassandraFlow.create(CassandraWriteSettings.defaults,
          "INSERT INTO test_keyspace.participants(name, value) VALUES (?, ?)",
          statementBinder)
      )
      .runWith(Sink.seq)

    write.onComplete {

      case Failure(f) => println("Cannot save record: " + f.toString())
    }
  }
}
