package com.airquality.application

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.airquality.domain.model._
import com.airquality.domain.service._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

case class ProcessingResult(
  originalReading: AirQualityReading,
  cleanedReading: Option[AirQualityReading],
  aqi: Option[AirQualityIndex],
  alerts: Seq[Alert],
  validationErrors: Seq[String] = Seq.empty
)

class AirQualityService(implicit ec: ExecutionContext) extends LazyLogging {
  
  def processingFlow: Flow[AirQualityReading, ProcessingResult, NotUsed] = {
    Flow[AirQualityReading]
      .map(processReading)
  }
  
  private def processReading(reading: AirQualityReading): ProcessingResult = {
    logger.debug(s"Traitement de la lecture: ${reading.sensorId}")
    
    // Validation
    val validationResult = Validation.validateReading(reading)
    val validationErrors = validationResult.fold(_.toList, _ => List.empty)
    
    // Nettoyage
    val cleanedReading = AirQualityTransformations.cleanReading(reading)
    
    // Calcul AQI
    val aqi = cleanedReading.flatMap(AirQualityTransformations.calculateAQI)
    
    // Génération d'alertes
    val alerts = cleanedReading.map(AlertRules.applyAllRules).getOrElse(Seq.empty)
    
    // Log des alertes
    alerts.foreach { alert =>
      logger.warn(s"Alerte générée: ${alert.severity.name} - ${alert.message}")
    }
    
    ProcessingResult(
      originalReading = reading,
      cleanedReading = cleanedReading,
      aqi = aqi,
      alerts = alerts,
      validationErrors = validationErrors
    )
  }
  
  def processReadings(readings: Seq[AirQualityReading]): Future[Seq[ProcessingResult]] = {
    Future {
      readings.map(processReading)
    }
  }
  
  def generateStatistics(readings: Seq[AirQualityReading]): Statistics = {
    StatisticsCalculator.computeStatistics(readings)
  }
}