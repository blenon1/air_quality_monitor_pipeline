package com.airquality.domain.model

import java.time.Instant

case class AirQualityIndex(
  value: Double,
  category: AQICategory,
  timestamp: Instant,
  dominantPollutant: Option[Pollutant] = None
)

sealed trait AQICategory {
  def name: String
  def description: String
  def colorCode: String
}

object AQICategory {
  case object Good extends AQICategory {
    val name = "Good"
    val description = "Air quality is satisfactory"
    val colorCode = "#00e400"
  }
  
  case object Moderate extends AQICategory {
    val name = "Moderate"
    val description = "Air quality is acceptable"
    val colorCode = "#ffff00"
  }
  
  case object UnhealthyForSensitive extends AQICategory {
    val name = "Unhealthy for Sensitive Groups"
    val description = "Sensitive individuals may experience health effects"
    val colorCode = "#ff7e00"
  }
  
  case object Unhealthy extends AQICategory {
    val name = "Unhealthy"
    val description = "Everyone may experience health effects"
    val colorCode = "#ff0000"
  }
  
  case object VeryUnhealthy extends AQICategory {
    val name = "Very Unhealthy"
    val description = "Health alert: serious effects for everyone"
    val colorCode = "#8f3f97"
  }
  
  case object Hazardous extends AQICategory {
    val name = "Hazardous"
    val description = "Emergency conditions affecting everyone"
    val colorCode = "#7e0023"
  }
}