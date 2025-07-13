package com.airquality.domain.service

import cats.data.ValidatedNel
import cats.implicits._
import com.airquality.domain.model._
import java.time.Instant

object Validation {
  
  type ValidationResult[A] = ValidatedNel[String, A]
  
  def validateReading(reading: AirQualityReading): ValidationResult[AirQualityReading] = {
    (
      validateSensorId(reading.sensorId),
      validateTimestamp(reading.timestamp),
      validateLocation(reading.location),
      validateMeasurements(reading.measurements)
    ).mapN((_, _, _, _) => reading)
  }
  
  def validateSensorId(sensorId: String): ValidationResult[String] = {
    if (sensorId.nonEmpty && sensorId.length <= 50) sensorId.validNel
    else "Sensor ID must be non-empty and less than 50 characters".invalidNel
  }
  
  def validateTimestamp(timestamp: Instant): ValidationResult[Instant] = {
    val now = Instant.now()
    val fiveMinutesAgo = now.minusSeconds(300)
    val fiveMinutesFromNow = now.plusSeconds(300)
    
    if (timestamp.isAfter(fiveMinutesAgo) && timestamp.isBefore(fiveMinutesFromNow)) {
      timestamp.validNel
    } else {
      "Timestamp must be within 5 minutes of current time".invalidNel
    }
  }
  
  def validateLocation(location: Location): ValidationResult[Location] = {
    (
      validateLatitude(location.latitude),
      validateLongitude(location.longitude)
    ).mapN((_, _) => location)
  }
  
  private def validateLatitude(lat: Double): ValidationResult[Double] = {
    if (lat >= -90.0 && lat <= 90.0) lat.validNel
    else s"Latitude must be between -90 and 90, got $lat".invalidNel
  }
  
  private def validateLongitude(lon: Double): ValidationResult[Double] = {
    if (lon >= -180.0 && lon <= 180.0) lon.validNel
    else s"Longitude must be between -180 and 180, got $lon".invalidNel
  }
  
  def validateMeasurements(measurements: Map[Pollutant, Double]): ValidationResult[Map[Pollutant, Double]] = {
    if (measurements.nonEmpty) {
      val validations = measurements.map { case (pollutant, value) =>
        validateMeasurement(pollutant, value)
      }.toList
      
      validations.sequence.map(_ => measurements)
    } else {
      "At least one measurement is required".invalidNel
    }
  }
  
  private def validateMeasurement(pollutant: Pollutant, value: Double): ValidationResult[Double] = {
    if (value >= 0.0 && value <= 1000.0) value.validNel
    else s"${pollutant.name} value must be between 0 and 1000, got $value".invalidNel
  }
}