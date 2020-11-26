mainClass in Compile := Some("Main")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

name := "TestProject"

version := "0.1"

scalaVersion := "2.13.3"

lazy val akkaVersion = "2.6.10"

lazy val kafkaVersion = "2.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % kafkaVersion
)


