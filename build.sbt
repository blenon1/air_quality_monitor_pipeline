// ThisBuild / version := "0.1.0-SNAPSHOT"
// ThisBuild / scalaVersion := "2.13.12"

// lazy val root = (project in file("."))
//   .settings(
//     name := "air-quality-monitor",
    
//     // Configuration Assembly
//     assembly / assemblyMergeStrategy := {
//       case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//       case _ => MergeStrategy.first
//     }
//   )

// ThisBuild / version := "0.1.0-SNAPSHOT"
// ThisBuild / scalaVersion := "2.13.12"

// lazy val root = (project in file("."))
//   .settings(
//     name := "air-quality-monitor",
    
//     libraryDependencies ++= Seq(
//       // Core Scala
//       "org.scala-lang" %% "scala-library" % scalaVersion.value,
      
//       // Collections parallèles
//       "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
      
//       // Configuration et Logging
//       "com.typesafe" % "config" % "1.4.2",
//       "ch.qos.logback" % "logback-classic" % "1.2.12",
//       "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      
//       // Tests
//       "org.scalatest" %% "scalatest" % "3.2.17" % Test
//     ),
    
//     // Configuration Assembly
//     assembly / assemblyMergeStrategy := {
//       case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//       case "application.conf" => MergeStrategy.concat
//       case "reference.conf" => MergeStrategy.concat
//       case _ => MergeStrategy.first
//     },
    
//     // Nom du JAR
//     assembly / assemblyJarName := s"${name.value}-assembly-${version.value}.jar"
//   )

// ThisBuild / version := "0.1.0-SNAPSHOT"
// ThisBuild / scalaVersion := "2.13.12"

// lazy val root = (project in file("."))
//   .settings(
//     name := "air-quality-monitor",
    
//     libraryDependencies ++= Seq(
//       // Collections parallèles
//       "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
      
//       // Configuration et Logging
//       "com.typesafe" % "config" % "1.4.2",
//       "ch.qos.logback" % "logback-classic" % "1.2.12",
//       "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      
//       // Tests
//       "org.scalatest" %% "scalatest" % "3.2.17" % Test
//     ),
    
//     // Configuration Assembly
//     assembly / assemblyMergeStrategy := {
//       case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//       case "application.conf" => MergeStrategy.concat
//       case "reference.conf" => MergeStrategy.concat
//       case _ => MergeStrategy.first
//     },
    
//     // Nom du JAR
//     assembly / assemblyJarName := s"${name.value}-assembly-${version.value}.jar"
//   )



ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "air-quality-monitor",
    
    libraryDependencies ++= Seq(
      // Configuration et Logging
      "com.typesafe" % "config" % "1.4.2",
      "ch.qos.logback" % "logback-classic" % "1.2.12",
      
      // Tests
      "org.scalatest" %% "scalatest" % "3.2.17" % Test
    ),
    
    // Spécifier la classe principale
    Compile / mainClass := Some("com.airquality.Main"),
    assembly / mainClass := Some("com.airquality.Main"),
    
    // Configuration Assembly
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    
    // Nom du JAR
    assembly / assemblyJarName := s"${name.value}-assembly-${version.value}.jar"
)