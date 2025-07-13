package com.airquality

import com.airquality.domain.model._
import java.time.Instant

object TestData {
  
  def validAirQualityReading(
    sensorId: String = "test-sensor-001",
    pm25: Double = 25.0,
    pm10: Double = 35.0,
    no2: Double = 40.0
  ): AirQualityReading = {
    AirQualityReading(
      sensorId = sensorId,
      timestamp = Instant.now(),
      location = Location(48.8566, 2.3522), // Paris
      measurements = Map(
        Pollutant.PM25 -> pm25,
        Pollutant.PM10 -> pm10,
        Pollutant.NO2 -> no2,
        Pollutant.O3 -> 80.0
      ),
      metadata = Map(
        "source" -> "test",
        "version" -> "1.0"
      )
    )
  }
  
  def invalidAirQualityReading(): AirQualityReading = {
    AirQualityReading(
      sensorId = "",
      timestamp = Instant.now().plusSeconds(3600), // Future timestamp
      location = Location(200.0, 300.0), // Invalid coordinates
      measurements = Map(
        Pollutant.PM25 -> -10.0, // Negative value
        Pollutant.PM10 -> 2000.0 // Too high value
      )
    )
  }
  
  def criticalPollutionReading(): AirQualityReading = {
    validAirQualityReading(
      sensorId = "critical-sensor",
      pm25 = 300.0, // Very high
      pm10 = 250.0,
      no2 = 250.0
    )
  }
  
  def sampleReadings(count: Int): Seq[AirQualityReading] = {
    (1 to count).map { i =>
      validAirQualityReading(
        sensorId = f"sensor-$i%03d",
        pm25 = 20.0 + (i % 10) * 5.0,
        pm10 = 30.0 + (i % 8) * 3.0,
        no2 = 35.0 + (i % 12) * 2.0
      )
    }
  }
}