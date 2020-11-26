import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

import Main.system

class Producer {

  private val config = system.settings.config.getConfig("akka.kafka.producer")

  private val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  private val producer = SendProducer(producerSettings)

  def createPaymentRecord(key: String, value: String) = {

    try {

      val send: Future[RecordMetadata] = producer.send(new ProducerRecord("transactions", key, value))

      Await.result(send, 2.seconds)

    } finally {

      Await.result(producer.close(), 1.minute)
    }
  }
}
