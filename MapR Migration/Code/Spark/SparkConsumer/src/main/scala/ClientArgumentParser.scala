/**
 * Created by John on 25/06/2017.
 */

object ClientArgumentKeys extends Enumeration {
  val AppName: String = "appName"
  val BootstrapServers: String = "bootstrapServers"
  val TopicName: String = "topicName"
}

object ClientArgumentParser {

  type ArgumentMap = Map[Symbol, Any]

  def usageExample() {

    val appName: String = "<appname>"
    val bootstrapServers: String = "<host1:port1,host2:port2,...>"
    val topicName: String = "<topicname>"

    println()
    println(s"Usage: --application $appName --bootstrap $bootstrapServers --topic $topicName")
    println()
  }

  def parseArguments(argumentMap: ArgumentMap, argumentList: List[String]): ArgumentMap = {

    argumentList match {
      case Nil => argumentMap
      case "--application" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.AppName) -> value.toString), tail)
      case "--bootstrap" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.BootstrapServers) -> value.toString), tail)
      case "--topic" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.TopicName) -> value.toString), tail)
      case option :: tail =>
        println()
        println("Unknown option: " + option)
        println()
        usageExample()
        sys.exit(1)
    }
  }

  def verifyArguments(argumentMap: ArgumentMap): Unit = {

    assert(argumentMap.contains(Symbol(ClientArgumentKeys.AppName)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.BootstrapServers)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.TopicName)))
  }
}
