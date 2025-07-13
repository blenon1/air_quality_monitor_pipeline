package com.airquality.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

case class AppConfig(
  apiKey: String,
  pollInterval: Duration,
  alertThresholds: Map[String, Double],
  server: ServerConfig
)

case class ServerConfig(
  host: String,
  port: Int
)

object AppConfig {
  
  def load(): AppConfig = {
    val config = ConfigFactory.load()
    
    Try {
      AppConfig(
        apiKey = config.getString("airquality.api.key"),
        pollInterval = Duration(config.getString("airquality.poll-interval")),
        alertThresholds = loadAlertThresholds(config),
        server = ServerConfig(
          host = config.getString("server.host"),
          port = config.getInt("server.port")
        )
      )
    } match {
      case Success(appConfig) => appConfig
      case Failure(exception) => 
        throw new RuntimeException(s"Failed to load configuration: ${exception.getMessage}", exception)
    }
  }
  
  private def loadAlertThresholds(config: Config): Map[String, Double] = {
    val thresholds = config.getConfig("airquality.alert-thresholds")
    Map(
      "pm25" -> thresholds.getDouble("pm25"),
      "pm10" -> thresholds.getDouble("pm10"),
      "no2" -> thresholds.getDouble("no2"),
      "o3" -> thresholds.getDouble("o3")
    )
  }
}