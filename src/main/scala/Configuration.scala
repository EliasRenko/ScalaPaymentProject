import Main.system

class Configuration() {

  val balance:Long = system.settings.config.getLong("scalaPaymentProject.balance")

  val sourceDir:String = system.settings.config.getString("scalaPaymentProject.sourceDirectory")

  val mask:String = system.settings.config.getString("scalaPaymentProject.mask")

  val kafkaProducerConfig = system.settings.config.getConfig("akka.kafka.producer")

  val kafkaConsumerConfig = system.settings.config.getConfig("akka.kafka.consumer")

  val kafkaHost:String = system.settings.config.getString("scalaPaymentProject.kafkaHost")

  val kafkaPort:String = system.settings.config.getString("scalaPaymentProject.kafkaPort")

  val transactionsGroup:String = system.settings.config.getString("scalaPaymentProject.transactionsGroup")

  val transactionsTopic:String = system.settings.config.getString("scalaPaymentProject.transactionsTopic")
}
