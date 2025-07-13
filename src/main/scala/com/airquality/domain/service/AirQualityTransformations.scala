package com.airquality.domain.service

import com.airquality.domain.model._
import java.time.Instant

object AirQualityTransformations {
  
  // Fonction pure pour calculer l'AQI
  def calculateAQI(reading: AirQualityReading): Option[AirQualityIndex] = {
    reading.measurements.get(Pollutant.PM25).map { pm25 =>
      val aqi = calculatePM25AQI(pm25)
      val category = determineCategory(aqi)
      AirQualityIndex(
        value = aqi,
        category = category,
        timestamp = reading.timestamp,
        dominantPollutant = Some(Pollutant.PM25)
      )
    }
  }
  
  // Calcul AQI basé sur PM2.5
  private def calculatePM25AQI(pm25: Double): Double = {
    pm25 match {
      case x if x <= 12.0 => (50.0 / 12.0) * x
      case x if x <= 35.4 => 50.0 + ((100.0 - 50.0) / (35.4 - 12.1)) * (x - 12.1)
      case x if x <= 55.4 => 100.0 + ((150.0 - 100.0) / (55.4 - 35.5)) * (x - 35.5)
      case x if x <= 150.4 => 150.0 + ((200.0 - 150.0) / (150.4 - 55.5)) * (x - 55.5)
      case x if x <= 250.4 => 200.0 + ((300.0 - 200.0) / (250.4 - 150.5)) * (x - 150.5)
      case x => 300.0 + ((500.0 - 300.0) / (500.4 - 250.5)) * (x - 250.5)
    }
  }
  
  // Détermination de la catégorie AQI
  private def determineCategory(aqi: Double): AQICategory = {
    aqi match {
      case x if x <= 50 => AQICategory.Good
      case x if x <= 100 => AQICategory.Moderate
      case x if x <= 150 => AQICategory.UnhealthyForSensitive
      case x if x <= 200 => AQICategory.Unhealthy
      case x if x <= 300 => AQICategory.VeryUnhealthy
      case _ => AQICategory.Hazardous
    }
  }
  
  // Fonction pure pour nettoyer les données
  def cleanReading(reading: AirQualityReading): Option[AirQualityReading] = {
    val validMeasurements = reading.measurements.filter {
      case (_, value) => value >= 0 && value < 1000
    }
    
    if (validMeasurements.nonEmpty && reading.sensorId.nonEmpty) {
      Some(reading.copy(measurements = validMeasurements))
    } else {
      None
    }
  }
  
  // Fonction pure pour enrichir avec données météo
  def enrichWithWeather(reading: AirQualityReading, temperature: Double, humidity: Double, windSpeed: Double): AirQualityReading = {
    val enrichedMetadata = reading.metadata ++ Map(
      "temperature" -> temperature.toString,
      "humidity" -> humidity.toString,
      "windSpeed" -> windSpeed.toString
    )
    reading.copy(metadata = enrichedMetadata)
  }
}