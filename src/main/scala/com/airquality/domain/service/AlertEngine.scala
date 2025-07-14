// package com.airquality.domain.service

// import com.airquality.domain.model._

// object AlertEngine {
  
//   type AlertRule = AirQualityReading => List[Alert]
  
//   // Règles d'alertes composables
//   val thresholdRules: List[AlertRule] = List(
//     criticalPM25Rule,
//     criticalPM10Rule,
//     criticalNO2Rule,
//     criticalO3Rule,
//     multiPollutantRule
//   )
  
//   // Règle pour PM2.5 critique
//   val criticalPM25Rule: AlertRule = reading => {
//     reading.measurements.get(Pollutant.PM25).toList.flatMap { pm25 =>
//       pm25 match {
//         case x if x > 250 => List(Alert(
//           severity = AlertSeverity.Emergency,
//           message = s"PM2.5 en urgence: ${x.formatted("%.1f")}μg/m³ (seuil: 250)",
//           location = reading.location,
//           timestamp = reading.timestamp,
//           pollutant = Some(Pollutant.PM25),
//           value = Some(x),
//           threshold = Some(250.0)
//         ))
//         case x if x > 150 => List(Alert(
//           severity = AlertSeverity.Critical,
//           message = s"PM2.5 critique: ${x.formatted("%.1f")}μg/m³ (seuil: 150)",
//           location = reading.location,
//           timestamp = reading.timestamp,
//           pollutant = Some(Pollutant.PM25),
//           value = Some(x),
//           threshold = Some(150.0)
//         ))
//         case x if x > 55 => List(Alert(
//           severity = AlertSeverity.Warning,
//           message = s"PM2.5 élevé: ${x.formatted("%.1f")}μg/m³ (seuil: 55)",
//           location = reading.location,
//           timestamp = reading.timestamp,
//           pollutant = Some(Pollutant.PM25),
//           value = Some(x),
//           threshold = Some(55.0)
//         ))
//         case _ => List.empty
//       }
//     }
//   }
  
//   // Règle pour PM10 critique
//   val criticalPM10Rule: AlertRule = reading => {
//     reading.measurements.get(Pollutant.PM10).toList.flatMap { pm10 =>
//       if (pm10 > 150) List(Alert(
//         severity = AlertSeverity.Critical,
//         message = s"PM10 critique: ${pm10.formatted("%.1f")}μg/m³",
//         location = reading.location,
//         timestamp = reading.timestamp,
//         pollutant = Some(Pollutant.PM10),
//         value = Some(pm10),
//         threshold = Some(150.0)
//       )) else List.empty
//     }
//   }
  
//   // Règle pour NO2 critique
//   val criticalNO2Rule: AlertRule = reading => {
//     reading.measurements.get(Pollutant.NO2).toList.flatMap { no2 =>
//       if (no2 > 200) List(Alert(
//         severity = AlertSeverity.Critical,
//         message = s"NO2 critique: ${no2.formatted("%.1f")}μg/m³",
//         location = reading.location,
//         timestamp = reading.timestamp,
//         pollutant = Some(Pollutant.NO2),
//         value = Some(no2),
//         threshold = Some(200.0)
//       )) else List.empty
//     }
//   }
  
//   // Règle pour O3 critique
//   val criticalO3Rule: AlertRule = reading => {
//     reading.measurements.get(Pollutant.O3).toList.flatMap { o3 =>
//       if (o3 > 180) List(Alert(
//         severity = AlertSeverity.Critical,
//         message = s"Ozone critique: ${o3.formatted("%.1f")}μg/m³",
//         location = reading.location,
//         timestamp = reading.timestamp,
//         pollutant = Some(Pollutant.O3),
//         value = Some(o3),
//         threshold = Some(180.0)
//       )) else List.empty
//     }
//   }
  
//   // Règle pour multi-polluants
//   val multiPollutantRule: AlertRule = reading => {
//     val exceededThresholds = reading.measurements.count { case (pollutant, value) =>
//       value > pollutant.safeThreshold
//     }
    
//     if (exceededThresholds >= 3) List(Alert(
//       severity = AlertSeverity.Critical,
//       message = s"Pollution multi-polluants: $exceededThresholds polluants dépassent les seuils",
//       location = reading.location,
//       timestamp = reading.timestamp
//     )) else List.empty
//   }
  
//   // Application de toutes les règles
//   def applyAllRules(reading: AirQualityReading): List[Alert] = {
//     thresholdRules.flatMap(_(reading))
//   }
  
//   // Détection de tendances alarmantes
//   def detectTrendAlerts(readings: List[AirQualityReading], pollutant: Pollutant): List[Alert] = {
//     if (readings.length < 5) List.empty
//     else {
//       val values = readings.flatMap(_.measurements.get(pollutant))
//       val trend = StatisticsCalculator.calculateTrend(values)
//       val recentAvg = StatisticsCalculator.mean(values.takeRight(3))
//       val overallAvg = StatisticsCalculator.mean(values)
      
//       (trend, recentAvg - overallAvg) match {
//         case (TrendDirection.Increasing, diff) if diff > 10 =>
//           readings.lastOption.toList.map { lastReading =>
//             Alert(
//               severity = AlertSeverity.Warning,
//               message = s"Tendance croissante détectée pour ${pollutant.name} (+${diff.formatted("%.1f")}${pollutant.unit})",
//               location = lastReading.location,
//               timestamp = lastReading.timestamp,
//               pollutant = Some(pollutant)
//             )
//           }
//         case _ => List.empty
//       }
//     }
//   }
  
//   // Filtrage et priorisation des alertes
//   def prioritizeAlerts(alerts: List[Alert]): List[Alert] = {
//     alerts
//       .groupBy(a => (a.location, a.pollutant))
//       .values
//       .map(_.maxBy(_.severity.level))
//       .toList
//       .sortBy(-_.severity.level)
//   }
  
//   // Agrégation d'alertes similaires
//   def aggregateSimilarAlerts(alerts: List[Alert], timeWindowMinutes: Int = 30): List[Alert] = {
//     val timeWindow = timeWindowMinutes * 60 * 1000 // en millisecondes
    
//     alerts
//       .groupBy(a => (a.severity, a.pollutant))
//       .values
//       .flatMap { group =>
//         group
//           .groupBy(a => a.timestamp.toEpochMilli / timeWindow)
//           .values
//           .map { timeGroup =>
//             if (timeGroup.length == 1) timeGroup.head
//             else Alert(
//               severity = timeGroup.head.severity,
//               message = s"${timeGroup.length} alertes similaires: ${timeGroup.head.message}",
//               location = timeGroup.head.location,
//               timestamp = timeGroup.map(_.timestamp).min,
//               pollutant = timeGroup.head.pollutant
//             )
//           }
//       }
//       .toList
//   }
// }


package com.airquality.domain.service

import com.airquality.domain.model._

object AlertEngine {
  
  type AlertRule = AirQualityReading => List[Alert]
  
  // Règle pour PM2.5 critique
  val criticalPM25Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.PM25).toList.flatMap { pm25 =>
      pm25 match {
        case x if x > 250 => List(Alert(
          severity = AlertSeverity.Emergency,
          message = s"PM2.5 en urgence: ${x.formatted("%.1f")}μg/m³ (seuil: 250)",
          location = reading.location,
          timestamp = reading.timestamp,
          pollutant = Some(Pollutant.PM25),
          value = Some(x),
          threshold = Some(250.0)
        ))
        case x if x > 150 => List(Alert(
          severity = AlertSeverity.Critical,
          message = s"PM2.5 critique: ${x.formatted("%.1f")}μg/m³ (seuil: 150)",
          location = reading.location,
          timestamp = reading.timestamp,
          pollutant = Some(Pollutant.PM25),
          value = Some(x),
          threshold = Some(150.0)
        ))
        case x if x > 55 => List(Alert(
          severity = AlertSeverity.Warning,
          message = s"PM2.5 élevé: ${x.formatted("%.1f")}μg/m³ (seuil: 55)",
          location = reading.location,
          timestamp = reading.timestamp,
          pollutant = Some(Pollutant.PM25),
          value = Some(x),
          threshold = Some(55.0)
        ))
        case _ => List.empty
      }
    }
  }
  
  // Règle pour PM10 critique
  val criticalPM10Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.PM10).toList.flatMap { pm10 =>
      if (pm10 > 150) List(Alert(
        severity = AlertSeverity.Critical,
        message = s"PM10 critique: ${pm10.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.PM10),
        value = Some(pm10),
        threshold = Some(150.0)
      )) else List.empty
    }
  }
  
  // Règle pour NO2 critique
  val criticalNO2Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.NO2).toList.flatMap { no2 =>
      if (no2 > 200) List(Alert(
        severity = AlertSeverity.Critical,
        message = s"NO2 critique: ${no2.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.NO2),
        value = Some(no2),
        threshold = Some(200.0)
      )) else List.empty
    }
  }
  
  // Règle pour O3 critique
  val criticalO3Rule: AlertRule = reading => {
    reading.measurements.get(Pollutant.O3).toList.flatMap { o3 =>
      if (o3 > 180) List(Alert(
        severity = AlertSeverity.Critical,
        message = s"Ozone critique: ${o3.formatted("%.1f")}μg/m³",
        location = reading.location,
        timestamp = reading.timestamp,
        pollutant = Some(Pollutant.O3),
        value = Some(o3),
        threshold = Some(180.0)
      )) else List.empty
    }
  }
  
  // Règle pour multi-polluants
  val multiPollutantRule: AlertRule = reading => {
    val exceededThresholds = reading.measurements.count { case (pollutant, value) =>
      value > pollutant.safeThreshold
    }
    
    if (exceededThresholds >= 3) List(Alert(
      severity = AlertSeverity.Critical,
      message = s"Pollution multi-polluants: $exceededThresholds polluants dépassent les seuils",
      location = reading.location,
      timestamp = reading.timestamp
    )) else List.empty
  }
  
  // Règles d'alertes composables (défini après les règles individuelles)
  val thresholdRules: List[AlertRule] = List(
    criticalPM25Rule,
    criticalPM10Rule,
    criticalNO2Rule,
    criticalO3Rule,
    multiPollutantRule
  )
  
  // Application de toutes les règles
  def applyAllRules(reading: AirQualityReading): List[Alert] = {
    thresholdRules.flatMap(_(reading))
  }
  
  // Détection de tendances alarmantes
  def detectTrendAlerts(readings: List[AirQualityReading], pollutant: Pollutant): List[Alert] = {
    if (readings.length < 5) List.empty
    else {
      val values = readings.flatMap(_.measurements.get(pollutant))
      val trend = StatisticsCalculator.calculateTrend(values)
      val recentAvg = StatisticsCalculator.mean(values.takeRight(3))
      val overallAvg = StatisticsCalculator.mean(values)
      
      (trend, recentAvg - overallAvg) match {
        case (TrendDirection.Increasing, diff) if diff > 10 =>
          readings.lastOption.toList.map { lastReading =>
            Alert(
              severity = AlertSeverity.Warning,
              message = s"Tendance croissante détectée pour ${pollutant.name} (+${diff.formatted("%.1f")}${pollutant.unit})",
              location = lastReading.location,
              timestamp = lastReading.timestamp,
              pollutant = Some(pollutant)
            )
          }
        case _ => List.empty
      }
    }
  }
  
  // Filtrage et priorisation des alertes
  def prioritizeAlerts(alerts: List[Alert]): List[Alert] = {
    alerts
      .groupBy(a => (a.location, a.pollutant))
      .values
      .map(_.maxBy(_.severity.level))
      .toList
      .sortBy(-_.severity.level)
  }
  
  // Agrégation d'alertes similaires
  def aggregateSimilarAlerts(alerts: List[Alert], timeWindowMinutes: Int = 30): List[Alert] = {
    val timeWindow = timeWindowMinutes * 60 * 1000 // en millisecondes
    
    alerts
      .groupBy(a => (a.severity, a.pollutant))
      .values
      .flatMap { group =>
        group
          .groupBy(a => a.timestamp.toEpochMilli / timeWindow)
          .values
          .map { timeGroup =>
            if (timeGroup.length == 1) timeGroup.head
            else Alert(
              severity = timeGroup.head.severity,
              message = s"${timeGroup.length} alertes similaires: ${timeGroup.head.message}",
              location = timeGroup.head.location,
              timestamp = timeGroup.map(_.timestamp).min,
              pollutant = timeGroup.head.pollutant
            )
          }
      }
      .toList
  }
}