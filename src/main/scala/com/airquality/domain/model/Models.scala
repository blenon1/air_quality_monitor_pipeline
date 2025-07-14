package com.airquality.domain.model

import java.time.Instant
import java.util.UUID

// Modèles immutables pour les données
case class AirQualityReading(
  sensorId: String,
  timestamp: Instant,
  location: Location,
  measurements: Map[Pollutant, Double],
  metadata: Map[String, String] = Map.empty
)

case class Location(latitude: Double, longitude: Double, city: Option[String] = None)

// Types algébriques pour les polluants
sealed trait Pollutant {
  def name: String
  def unit: String
  def safeThreshold: Double
}

object Pollutant {
  case object PM25 extends Pollutant {
    val name = "PM2.5"
    val unit = "μg/m³"
    val safeThreshold = 25.0
  }
  
  case object PM10 extends Pollutant {
    val name = "PM10"
    val unit = "μg/m³"
    val safeThreshold = 50.0
  }
  
  case object NO2 extends Pollutant {
    val name = "NO2"
    val unit = "μg/m³"
    val safeThreshold = 100.0
  }
  
  case object O3 extends Pollutant {
    val name = "O3"
    val unit = "μg/m³"
    val safeThreshold = 120.0
  }
  
  case object CO extends Pollutant {
    val name = "CO"
    val unit = "mg/m³"
    val safeThreshold = 10.0
  }
  
  val all: List[Pollutant] = List(PM25, PM10, NO2, O3, CO)
  
  def fromString(name: String): Option[Pollutant] = all.find(_.name.equalsIgnoreCase(name))
}

// Index de qualité de l'air
case class AirQualityIndex(
  value: Double,
  category: AQICategory,
  timestamp: Instant,
  dominantPollutant: Option[Pollutant] = None
)

sealed trait AQICategory {
  def level: Int
  def name: String
  def description: String
  def colorCode: String
}

object AQICategory {
  case object Good extends AQICategory {
    val level = 1
    val name = "Bon"
    val description = "Qualité de l'air satisfaisante"
    val colorCode = "#00E400"
  }
  
  case object Moderate extends AQICategory {
    val level = 2
    val name = "Modéré"
    val description = "Qualité de l'air acceptable"
    val colorCode = "#FFFF00"
  }
  
  case object UnhealthyForSensitive extends AQICategory {
    val level = 3
    val name = "Mauvais pour groupes sensibles"
    val description = "Risques pour personnes sensibles"
    val colorCode = "#FF7E00"
  }
  
  case object Unhealthy extends AQICategory {
    val level = 4
    val name = "Mauvais"
    val description = "Risques pour tous"
    val colorCode = "#FF0000"
  }
  
  case object VeryUnhealthy extends AQICategory {
    val level = 5
    val name = "Très mauvais"
    val description = "Alerte sanitaire"
    val colorCode = "#8F3F97"
  }
  
  case object Hazardous extends AQICategory {
    val level = 6
    val name = "Dangereux"
    val description = "Urgence sanitaire"
    val colorCode = "#7E0023"
  }
}

// Alertes
case class Alert(
  id: String = UUID.randomUUID().toString,
  severity: AlertSeverity,
  message: String,
  location: Location,
  timestamp: Instant,
  pollutant: Option[Pollutant] = None,
  value: Option[Double] = None,
  threshold: Option[Double] = None
)

sealed trait AlertSeverity {
  def level: Int
  def name: String
}

object AlertSeverity {
  case object Info extends AlertSeverity {
    val level = 1
    val name = "Info"
  }
  
  case object Warning extends AlertSeverity {
    val level = 2
    val name = "Attention"
  }
  
  case object Critical extends AlertSeverity {
    val level = 3
    val name = "Critique"
  }
  
  case object Emergency extends AlertSeverity {
    val level = 4
    val name = "Urgence"
  }
}

// Statistiques
case class PollutantStats(
  mean: Double,
  median: Double,
  min: Double,
  max: Double,
  stddev: Double,
  count: Int,
  percentiles: Map[Int, Double] = Map.empty
)

case class Statistics(
  timestamp: Instant,
  sampleSize: Int,
  pollutantStats: Map[Pollutant, PollutantStats],
  timeWindow: String,
  locationStats: Map[String, PollutantStats] = Map.empty
)

// Résultat de traitement
case class ProcessingResult(
  originalReading: AirQualityReading,
  cleanedReading: Option[AirQualityReading],
  aqi: Option[AirQualityIndex],
  alerts: Seq[Alert],
  validationErrors: Seq[String] = Seq.empty,
  processingTime: Long = 0L
)