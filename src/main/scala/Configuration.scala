import com.typesafe.config.ConfigFactory

class Configuration() {

  private val _config = ConfigFactory.load("application.conf").getConfig("scalaPaymentProject")

  val balance:Long = _config.getLong("balance")

  val sourceFile:String = _config.getString("sourceFile")

  val mask:String = _config.getString("mask")
}
