package com.airquality.application

import com.airquality.domain.model._
import com.airquality.domain.service._
import java.time.{Instant, LocalDate, ZoneId}

case class DailyReport(
  date: LocalDate,
  statistics: Statistics,
  alerts: Seq[Alert],
  trends: Map[Pollutant, TrendAnalysis],
  recommendations: Seq[Recommendation],
  summary: ReportSummary
)

case class TrendAnalysis(
  pollutant: Pollutant,
  direction: TrendDirection,
  magnitude: Double,
  confidence: Double
)

sealed trait TrendDirection
case object Increasing extends TrendDirection
case object Decreasing extends TrendDirection
case object Stable extends TrendDirection

case class Recommendation(
  priority: RecommendationPriority,
  title: String,
  description: String,
  actions: Seq[String]
)

sealed trait RecommendationPriority
case object High extends RecommendationPriority
case object Medium extends RecommendationPriority
case object Low extends RecommendationPriority

case class ReportSummary(
  totalReadings: Int,
  validReadings: Int,
  totalAlerts: Int,
  criticalAlerts: Int,
  averageAQI: Option[Double],
  dominantPollutant: Option[Pollutant]
)

object ReportGenerator {
  
  // Fonction pure pour générer un rapport journalier
  def generateDailyReport(readings: Seq[AirQualityReading]): DailyReport = {
    val statistics = StatisticsCalculator.computeStatistics(readings, "daily")
    val allAlerts = readings.flatMap(AlertRules.applyAllRules)
    val trends = analyzeTrends(readings)
    val recommendations = generateRecommendations(statistics, allAlerts, trends)
    val summary = generateSummary(readings, allAlerts)
    
    DailyReport(
      date = LocalDate.now(),
      statistics = statistics,
      alerts = allAlerts,
      trends = trends,
      recommendations = recommendations,
      summary = summary
    )
  }
  
  // Analyse des tendances
  private def analyzeTrends(readings: Seq[AirQualityReading]): Map[Pollutant, TrendAnalysis] = {
    Pollutant.all.map { pollutant =>
      val values = readings.flatMap(_.measurements.get(pollutant))
      if (values.length >= 10) {
        val trend = calculateTrend(values)
        pollutant -> trend
      } else {
        pollutant -> TrendAnalysis(pollutant, Stable, 0.0, 0.0)
      }
    }.toMap
  }
  
  private def calculateTrend(values: Seq[Double]): TrendAnalysis = {
    if (values.length < 2) {
      TrendAnalysis(Pollutant.PM25, Stable, 0.0, 0.0) // placeholder
    } else {
      val n = values.length
      val xValues = (1 to n).map(_.toDouble)
      val xMean = xValues.sum / n
      val yMean = values.sum / n
      
      val numerator = xValues.zip(values).map { case (x, y) => 
        (x - xMean) * (y - yMean) 
      }.sum
      
      val denominator = xValues.map(x => math.pow(x - xMean, 2)).sum
      
      val slope = if (denominator != 0) numerator / denominator else 0.0
      
      val direction = if (slope > 0.5) Increasing
                     else if (slope < -0.5) Decreasing
                     else Stable
      
      val confidence = math.min(1.0, math.abs(slope) / values.max)
      
      TrendAnalysis(Pollutant.PM25, direction, slope, confidence) // placeholder pollutant
    }
  }
  
  // Génération de recommandations
  private def generateRecommendations(
    statistics: Statistics, 
    alerts: Seq[Alert],
    trends: Map[Pollutant, TrendAnalysis]
  ): Seq[Recommendation] = {
    
    val criticalAlerts = alerts.count(_.severity == AlertSeverity.Critical)
    val emergencyAlerts = alerts.count(_.severity == AlertSeverity.Emergency)
    
    var recommendations = Seq.empty[Recommendation]
    
    // Recommandations basées sur les alertes critiques
    if (emergencyAlerts > 0) {
      recommendations = recommendations :+ Recommendation(
        priority = High,
        title = "Situation d'urgence détectée",
        description = s"$emergencyAlerts alertes d'urgence ont été émises",
        actions = Seq(
          "Éviter toute activité extérieure",
          "Fermer les fenêtres",
          "Utiliser un purificateur d'air",
          "Consulter un médecin si symptômes"
        )
      )
    }
    
    if (criticalAlerts > 5) {
      recommendations = recommendations :+ Recommendation(
        priority = High,
        title = "Pollution élevée persistante",
        description = s"$criticalAlerts alertes critiques enregistrées",
        actions = Seq(
          "Limiter les activités extérieures",
          "Porter un masque en sortant",
          "Surveiller la qualité de l'air"
        )
      )
    }
    
    // Recommandations basées sur les tendances
    trends.foreach { case (pollutant, trend) =>
      if (trend.direction == Increasing && trend.confidence > 0.7) {
        recommendations = recommendations :+ Recommendation(
          priority = Medium,
          title = s"Tendance croissante pour ${pollutant.name}",
          description = s"Une augmentation significative de ${pollutant.name} est observée",
          actions = Seq(
            s"Surveiller l'évolution de ${pollutant.name}",
            "Identifier les sources potentielles",
            "Prévoir des mesures préventives"
          )
        )
      }
    }
    
    // Recommandations générales
    val avgPM25 = statistics.pollutantStats.get(Pollutant.PM25).map(_.mean)
    avgPM25 match {
      case Some(pm25) if pm25 > 35 =>
        recommendations = recommendations :+ Recommendation(
          priority = Medium,
          title = "Qualité de l'air dégradée",
          description = s"PM2.5 moyen de ${pm25.formatted("%.1f")}μg/m³",
          actions = Seq(
            "Privilégier les transports en commun",
            "Éviter les activités sportives intenses",
            "Aérer aux heures de moindre pollution"
          )
        )
      case _ => // Pas de recommandation nécessaire
    }
    
    recommendations
  }
  
  private def generateSummary(readings: Seq[AirQualityReading], alerts: Seq[Alert]): ReportSummary = {
    val validReadings = readings.count(r => r.measurements.nonEmpty && r.sensorId.nonEmpty)
    val criticalAlerts = alerts.count(_.severity == AlertSeverity.Critical)
    
    val avgAQI = {
      val aqiValues = readings.flatMap(AirQualityTransformations.calculateAQI).map(_.value)
      if (aqiValues.nonEmpty) Some(aqiValues.sum / aqiValues.length) else None
    }
    
    val dominantPollutant = findDominantPollutant(readings)
    
    ReportSummary(
      totalReadings = readings.length,
      validReadings = validReadings,
      totalAlerts = alerts.length,
      criticalAlerts = criticalAlerts,
      averageAQI = avgAQI,
      dominantPollutant = dominantPollutant
    )
  }
  
  private def findDominantPollutant(readings: Seq[AirQualityReading]): Option[Pollutant] = {
    val pollutantCounts = readings
      .flatMap(_.measurements.keys)
      .groupBy(identity)
      .view
      .mapValues(_.size)
      .toMap
    
    if (pollutantCounts.nonEmpty) {
      Some(pollutantCounts.maxBy(_._2)._1)
    } else {
      None
    }
  }
  
  // Génération de rapport en format texte
  def generateTextReport(report: DailyReport): String = {
    val sb = new StringBuilder
    
    sb.append(s"=== RAPPORT QUALITÉ DE L'AIR - ${report.date} ===\n\n")
    
    // Résumé
    sb.append("RÉSUMÉ EXÉCUTIF\n")
    sb.append("---------------\n")
    sb.append(s"Lectures totales: ${report.summary.totalReadings}\n")
    sb.append(s"Lectures valides: ${report.summary.validReadings}\n")
    sb.append(s"Alertes générées: ${report.summary.totalAlerts}\n")
    sb.append(s"Alertes critiques: ${report.summary.criticalAlerts}\n")
    report.summary.averageAQI.foreach(aqi => 
      sb.append(s"AQI moyen: ${aqi.formatted("%.1f")}\n")
    )
    sb.append("\n")
    
    // Statistiques
    sb.append("STATISTIQUES DÉTAILLÉES\n")
    sb.append("-----------------------\n")
    report.statistics.pollutantStats.foreach { case (pollutant, stats) =>
      sb.append(s"${pollutant.name}:\n")
      sb.append(s"  Moyenne: ${stats.mean.formatted("%.2f")} ${pollutant.unit}\n")
      sb.append(s"  Min/Max: ${stats.min.formatted("%.2f")} / ${stats.max.formatted("%.2f")} ${pollutant.unit}\n")
      sb.append(s"  Écart-type: ${stats.stddev.formatted("%.2f")}\n")
      sb.append(s"  Échantillons: ${stats.count}\n\n")
    }
    
    // Alertes
    if (report.alerts.nonEmpty) {
      sb.append("ALERTES\n")
      sb.append("-------\n")
      report.alerts.groupBy(_.severity).foreach { case (severity, alerts) =>
        sb.append(s"${severity.name}: ${alerts.length} alertes\n")
        alerts.take(3).foreach { alert =>
          sb.append(s"  - ${alert.message}\n")
        }
        if (alerts.length > 3) {
          sb.append(s"  ... et ${alerts.length - 3} autres\n")
        }
        sb.append("\n")
      }
    }
    
    // Recommandations
    if (report.recommendations.nonEmpty) {
      sb.append("RECOMMANDATIONS\n")
      sb.append("---------------\n")
      report.recommendations.foreach { rec =>
        sb.append(s"[${rec.priority}] ${rec.title}\n")
        sb.append(s"${rec.description}\n")
        rec.actions.foreach(action => sb.append(s"  • $action\n"))
        sb.append("\n")
      }
    }
    
    sb.toString()
  }