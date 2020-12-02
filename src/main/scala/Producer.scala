import Main.system
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

class Producer {

  private val config = Main.configuration.kafkaProducerConfig

  private val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
    .withBootstrapServers(Main.configuration.kafkaHost + ":" + Main.configuration.kafkaPort)

  private val producer = SendProducer(producerSettings)

  def createTransactionRecord(key: String, value: String) = {

    val record: Future[RecordMetadata] = producer.send(new ProducerRecord(Main.configuration.transactionsTopic, key, value))
  }
}
