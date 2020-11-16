import Main.system

class Configuration() {

  val balance:Long = system.settings.config.getLong("scalaPaymentProject.balance")

  val sourceFile:String = system.settings.config.getString("scalaPaymentProject.sourceDirectory")

  val mask:String = system.settings.config.getString("scalaPaymentProject.mask")
}
