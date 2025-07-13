package com.airquality.infrastructure.metrics

import java.time.Instant
import scala.concurrent.duration.Duration

// Events pour le système de métriques
sealed trait ProcessingEvent
case class ReadingProcessed(duration: Duration) extends ProcessingEvent
case object AlertGenerated extends ProcessingEvent
case object ErrorOccurred extends ProcessingEvent
case class ValidationFailed(errors: Seq[String]) extends ProcessingEvent

// Compteurs immutables
case class ProcessingMetrics(
  readingsProcessed: Long = 0,
  alertsGenerated: Long = 0,
  errors: Long = 0,
  validationErrors: Long = 0,
  averageProcessingTime: Duration = Duration.Zero,
  lastUpdate: Instant = Instant.now()
)

object Metrics {
  
  // Fonction pure pour mettre à jour les métriques
  def updateMetrics(current: ProcessingMetrics, event: ProcessingEvent): ProcessingMetrics = {
    event match {
      case ReadingProcessed(duration) => 
        val newCount = current.readingsProcessed + 1
        val newAverage = updateAverageTime(current.averageProcessingTime, duration, newCount)
        current.copy(
          readingsProcessed = newCount,
          averageProcessingTime = newAverage,
          lastUpdate = Instant.now()
        )
        
      case AlertGenerated => 
        current.copy(
          alertsGenerated = current.alertsGenerated + 1,
          lastUpdate = Instant.now()
        )
        
      case ErrorOccurred => 
        current.copy(
          errors = current.errors + 1,
          lastUpdate = Instant.now()
        )
        
      case ValidationFailed(_) => 
        current.copy(
          validationErrors = current.validationErrors + 1,
          lastUpdate = Instant.now()
        )
    }
  }
  
  private def updateAverageTime(currentAverage: Duration, newDuration: Duration, count: Long): Duration = {
    if (count == 1) newDuration
    else {
      val totalTime = currentAverage.toNanos * (count - 1) + newDuration.toNanos
      Duration.fromNanos(totalTime / count)
    }
  }
  
  // Génération d'un rapport de métriques
  def generateMetricsReport(metrics: ProcessingMetrics): String = {
    s"""
       |=== MÉTRIQUES DE PERFORMANCE ===
       |Lectures traitées: ${metrics.readingsProcessed}
       |Alertes générées: ${metrics.alertsGenerated}
       |Erreurs: ${metrics.errors}
       |Erreurs de validation: ${metrics.validationErrors}
       |Temps de traitement moyen: ${metrics.averageProcessingTime.toMillis}ms
       |Dernière mise à jour: ${metrics.lastUpdate}
       |
       |Taux de succès: ${calculateSuccessRate(metrics)}%
       |Taux d'alertes: ${calculateAlertRate(metrics)}%
       |""".stripMargin
  }
  
  private def calculateSuccessRate(metrics: ProcessingMetrics): Double = {
    if (metrics.readingsProcessed == 0) 100.0
    else {
      val successfulReadings = metrics.readingsProcessed - metrics.errors - metrics.validationErrors
      (successfulReadings.toDouble / metrics.readingsProcessed) * 100.0
    }
  }
  
  private def calculateAlertRate(metrics: ProcessingMetrics): Double = {
    if (metrics.readingsProcessed == 0) 0.0
    else (metrics.alertsGenerated.toDouble / metrics.readingsProcessed) * 100.0
  }
}