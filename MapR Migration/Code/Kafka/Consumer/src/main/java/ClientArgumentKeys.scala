object ClientArgumentKeys extends Enumeration {
  val BootstrapServers: String = "bootstrapServers"
  val TopicName: String = "topicName"
  val Partition: String = "partition"
}

object ClientArgumentParser {

  type ArgumentMap = Map[Symbol, Any]

  def usageExample(): Unit = {

    val bootstrapServers: String = "<bootstrap-server1,bootstrap-server2,...>"
    val topicName: String = "<topic>"

    println()
    println(s"Usage: --bootstrap-servers $bootstrapServers" +
      s" --topic $topicName")
    println()
  }

  def parseArguments(argumentMap: ArgumentMap, argumentList: List[String]): ArgumentMap = {

    argumentList match {
      case Nil => argumentMap
      case "--bootstrap-servers" :: value :: tail =>
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

    assert(argumentMap.contains(Symbol(ClientArgumentKeys.BootstrapServers)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.TopicName)))
  }
}

