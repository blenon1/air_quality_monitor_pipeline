package com.airquality.domain.service

import com.airquality.domain.model._

object AlertRules {
  
  type AlertRule = AirQualityReading => Option[Alert]
  
  // Règle pour seuil critique PM2.5
  val criticalPM25Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.PM25) match {
      case Some(pm25) if pm25 > 250 => Some(Alert(
        severity = AlertSeverity.Emergency,
        message = s"PM2.5 critique détecté: ${pm25.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.PM25),
        value = Some(pm25)
      ))
      case Some(pm25) if pm25 > 150 => Some(Alert(
        severity = AlertSeverity.Critical,
        message = s"PM2.5 très élevé: ${pm25.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.PM25),
        value = Some(pm25)
      ))
      case _ => None
    }
  }
  
  // Règle pour seuil critique NO2
  val criticalNO2Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.NO2) match {
      case Some(no2) if no2 > 200 => Some(Alert(
        severity = AlertSeverity.Critical,
        message = s"NO2 critique: ${no2.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.NO2),
        value = Some(no2)
      ))
      case _ => None
    }
  }
  
  // Règle pour qualité de l'air générale
  val generalAirQualityRule: AlertRule = reading => {
    AirQualityTransformations.calculateAQI(reading) match {
      case Some(aqi) if aqi.value > 200 => Some(Alert(
        severity = AlertSeverity.Warning,
        message = s"Qualité de l'air dégradée (AQI: ${aqi.value.formatted("%.0f")})",
        location = reading.location,
        timestamp = reading.timestamp
      ))
      case _ => None
    }
  }
  
  // Composition des règles
  def applyAllRules(reading: AirQualityReading): Seq[Alert] = {
    val rules = Seq(criticalPM25Rule, criticalNO2Rule, generalAirQualityRule)
    rules.flatMap(_(reading))
  }
  
  // Analyse de tendance (nécessite historique)
  def detectTrend(readings: Seq[AirQualityReading], pollutant: Pollutant): Option[Alert] = {
    if (readings.length < 3) None
    else {
      val values = readings.flatMap(_.measurements.get(pollutant))
      if (values.length >= 3) {
        val recentValues = values.takeRight(3)
        val isIncreasing = recentValues.zip(recentValues.tail).forall { case (a, b) => b > a }
        val averageIncrease = if (recentValues.length >= 2) {
          (recentValues.last - recentValues.head) / (recentValues.length - 1)
        } else 0.0
        
        if (isIncreasing && averageIncrease > 10) {
          readings.lastOption.map { lastReading =>
            Alert(
              severity = AlertSeverity.Warning,
              message = s"Tendance croissante détectée pour ${pollutant.name} (+${averageIncrease.formatted("%.1f")}${pollutant.unit}/mesure)",
              location = lastReading.location,
              timestamp = lastReading.timestamp,
              pollutant = Some(pollutant)
            )
          }
        } else None
      } else None
    }
  }
}