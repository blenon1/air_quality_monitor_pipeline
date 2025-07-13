ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "air-quality-monitor",
    libraryDependencies ++= Seq(
      // Akka Streams
      "com.typesafe.akka" %% "akka-stream" % "2.8.5",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.5",
      
      // Cats for functional programming
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.2",
      
      // HTTP client
      "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.1",
      
      // JSON parsing
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      
      // Configuration
      "com.typesafe" % "config" % "1.4.3",
      
      // Logging
      "ch.qos.logback" % "logback-classic" % "1.4.11",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      
      // Testing
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.17.0" % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.8.5" % Test
    ),
    
    // Compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xlint"
    )
  )