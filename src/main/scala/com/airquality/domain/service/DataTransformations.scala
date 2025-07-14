package com.airquality.domain.service

import com.airquality.domain.model._
import java.time.Instant

object DataTransformations {
  
  // Fonction pure pour calculer l'AQI basé sur PM2.5
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
  
  // Formule EPA pour PM2.5
  private def calculatePM25AQI(pm25: Double): Double = {
    val breakpoints = List(
      (0.0, 12.0, 0, 50),
      (12.1, 35.4, 51, 100),
      (35.5, 55.4, 101, 150),
      (55.5, 150.4, 151, 200),
      (150.5, 250.4, 201, 300),
      (250.5, 500.4, 301, 500)
    )
    
    breakpoints.find { case (low, high, _, _) => pm25 >= low && pm25 <= high } match {
      case Some((cLow, cHigh, iLow, iHigh)) =>
        ((iHigh - iLow) / (cHigh - cLow)) * (pm25 - cLow) + iLow
      case None if pm25 > 500.4 => 500.0
      case None => 0.0
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
  
  // Nettoyage des données - fonction pure
  def cleanReading(reading: AirQualityReading): Option[AirQualityReading] = {
    for {
      cleanedMeasurements <- cleanMeasurements(reading.measurements)
      _ <- validateSensorId(reading.sensorId)
      _ <- validateTimestamp(reading.timestamp)
      _ <- validateLocation(reading.location)
    } yield reading.copy(measurements = cleanedMeasurements)
  }
  
  private def cleanMeasurements(measurements: Map[Pollutant, Double]): Option[Map[Pollutant, Double]] = {
    val cleaned = measurements.filter { case (_, value) => 
      value >= 0 && value < 1000 && !value.isNaN && !value.isInfinite
    }
    if (cleaned.nonEmpty) Some(cleaned) else None
  }
  
  private def validateSensorId(sensorId: String): Option[String] = 
    if (sensorId.nonEmpty && sensorId.length <= 50) Some(sensorId) else None
  
  private def validateTimestamp(timestamp: Instant): Option[Instant] = {
    val now = Instant.now()
    val fiveMinutesAgo = now.minusSeconds(300)
    val fiveMinutesFromNow = now.plusSeconds(300)
    
    if (timestamp.isAfter(fiveMinutesAgo) && timestamp.isBefore(fiveMinutesFromNow))
      Some(timestamp)
    else
      None
  }
  
  private def validateLocation(location: Location): Option[Location] = {
    if (location.latitude >= -90.0 && location.latitude <= 90.0 &&
        location.longitude >= -180.0 && location.longitude <= 180.0)
      Some(location)
    else
      None
  }
  
  // Enrichissement avec données contextuelles
  def enrichWithContext(reading: AirQualityReading, weatherData: Map[String, Double]): AirQualityReading = {
    val enrichedMetadata = reading.metadata ++ weatherData.map { case (k, v) => k -> v.toString }
    reading.copy(metadata = enrichedMetadata)
  }
  
  // Normalisation des données
  def normalizeReading(reading: AirQualityReading): AirQualityReading = {
    val normalizedMeasurements = reading.measurements.map { case (pollutant, value) =>
      pollutant -> (value / pollutant.safeThreshold)
    }
    reading.copy(measurements = normalizedMeasurements)
  }
}
