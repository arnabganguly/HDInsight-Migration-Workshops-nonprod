name := "Spark Consumer"
version := "1.0"
scalaVersion := "2.11.10"
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-tags" % "2.4.0"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.1.1"

assemblyJarName in assembly := "SparkConsumer.jar" 

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
