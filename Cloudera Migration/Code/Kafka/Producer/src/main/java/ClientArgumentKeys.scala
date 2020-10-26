object ClientArgumentKeys extends Enumeration {
  val SourceFilename: String = "sourceFilename"
  val BootstrapServers: String = "bootstrapServers"
  val TopicName: String = "topicName"
  val NumPartitions: String = "numPartitions"
}

object ClientArgumentParser {

  type ArgumentMap = Map[Symbol, Any]

  def usageExample(): Unit = {

    val sourceFilename: String = "<sourcefilename>"
    val bootstrapServers: String = "<bootstrap-server1,bootstrap-server2,...>"
    val topicName: String = "<topic>"
    val numPartitions: String = "<partitions>"

    println()
    println(s"Usage: --sourcefilename $sourceFilename  --bootstrap-servers $bootstrapServers" +
      s" --topic $topicName --partitions $numPartitions")
    println()
  }

  def parseArguments(argumentMap: ArgumentMap, argumentList: List[String]): ArgumentMap = {

    argumentList match {
      case Nil => argumentMap
      case "--sourcefilename" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.SourceFilename) -> value.toString), tail)
      case "--bootstrap-servers" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.BootstrapServers) -> value.toString), tail)
      case "--topic" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.TopicName) -> value.toString), tail)
      case "--partitions" :: value :: tail =>
        parseArguments(argumentMap ++ Map(Symbol(ClientArgumentKeys.NumPartitions) -> value.toInt), tail)
      case option :: tail =>
        println()
        println("Unknown option: " + option)
        println()
        usageExample()
        sys.exit(1)
    }
  }

  def verifyArguments(argumentMap: ArgumentMap): Unit = {

    assert(argumentMap.contains(Symbol(ClientArgumentKeys.SourceFilename)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.BootstrapServers)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.TopicName)))
    assert(argumentMap.contains(Symbol(ClientArgumentKeys.NumPartitions)))
  }
}
