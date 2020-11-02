import com.typesafe.config.ConfigFactory

import scala.jdk.CollectionConverters.CollectionHasAsScala

class Configuration() {

  println("Configuration initiated!");

  val _config = ConfigFactory.load("application.conf").getConfig("testProject").getConfig("payments")

  // ** Defs.

  def getBalance():Long = {

    //_config.getString("value")

    val _value:Long = _config.getLong("balance")

    _value
  }

  def getData():String = {

    //_config.getString("value")

    val _value:String = _config.getString("data")

    _value
  }

  def getMask():String = {

    val _value:String = _config.getString("mask")

    _value
  }
}
