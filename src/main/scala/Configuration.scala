import Main.system

class Configuration() {

  val balance:Long = system.settings.config.getLong("scalaPaymentProject.balance")

  val sourceFile:String = system.settings.config.getString("scalaPaymentProject.sourceDirectory")

  val mask:String = system.settings.config.getString("scalaPaymentProject.mask")

  val kafkaConsumerConfig = system.settings.config.getConfig("akka.kafka.consumer")

  val kafkaHost:String = system.settings.config.getString("scalaPaymentProject.kafkaHost")

  val kafkaPort:String = system.settings.config.getString("scalaPaymentProject.kafkaPort")

  val transactionsGroup:String = system.settings.config.getString("scalaPaymentProject.transactionsGroup")

  val transactionsTopic:String = system.settings.config.getString("scalaPaymentProject.transactionsTopic")
}
