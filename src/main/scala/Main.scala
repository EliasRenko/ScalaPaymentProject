import java.io.File

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.FileIO

import scala.concurrent.Future

object Main extends App {

  implicit val system: ActorSystem = ActorSystem("default")

  val configuration:Configuration = new Configuration()

  private val paymentChecker:ActorRef = createPaymentChecker()

  //private val paymentReader:ActorRef = createPaymentReader(configuration.sourceFile, paymentChecker)

  private val paymentReader:ActorRef = createPaymentReaderKafkaJson(paymentChecker)

  //private val paymentReader:ActorRef = createPaymentReaderCassandra(configuration.sourceDir, paymentChecker)

  createTransactionRecord()

  paymentReader ! StartReading

  case object StartReading

  protected def createPaymentChecker(): ActorRef = {

    system.actorOf(PaymentChecker.props())
  }

  protected def createPaymentReader(source:String, checkerRef:ActorRef): ActorRef = {

    system.actorOf(PaymentReader.props(source, checkerRef))
  }

  protected def createPaymentReaderKafkaJson(checkerRef:ActorRef): ActorRef = {

    system.actorOf(PaymentReaderKafkaJson.props(checkerRef))
  }

  protected def createPaymentReaderCassandra(directory:String, checkerRef:ActorRef): ActorRef = {

    system.actorOf(PaymentReaderCassandra.props(directory, checkerRef))
  }

  protected def createTransactionRecord():Unit = {

    val producer:Producer = new Producer()

    val sourceFiles:List[File] = getSourceFiles(Main.configuration.sourceDir)

    for (file <- sourceFiles) {

        val checkFiles:Future[Done] = FileIO.fromPath(file.toPath).map(_.utf8String)
          .runForeach(i => producer.createTransactionRecord(null, i))
      }
  }

  private def getSourceFiles(source:String):List[File] = {

    val sourceDirectory = new File(source)

    if (sourceDirectory.exists && sourceDirectory.isDirectory) {

      sourceDirectory.listFiles.filter(_.isFile).toList
    }
    else {

      List[File]()
    }
  }
}
