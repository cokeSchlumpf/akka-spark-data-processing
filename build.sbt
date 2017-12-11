name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.11.12"

lazy val akkaVersion = "2.4.20"

libraryDependencies ++= Seq(
  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.0-akka-2.4.x",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  "org.apache.spark" %% "spark-sql" % "2.2.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.twitter4j" % "twitter4j-core" % "4.0.6"
)
