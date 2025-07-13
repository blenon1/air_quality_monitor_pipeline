package com.airquality.domain.model

import java.time.Instant

case class AirQualityReading(
  sensorId: String,
  timestamp: Instant,
  location: Location,
  measurements: Map[Pollutant, Double],
  metadata: Map[String, String] = Map.empty
)