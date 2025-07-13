package com.airquality.domain.pipeline

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.airquality.domain.model._
import com.airquality.domain.service._
import com.airquality.application.ProcessingResult

import scala.concurrent.duration._

object AirQualityPipeline {
  
  // Pipeline principal de traitement
  def processingPipeline: Flow[AirQualityReading, ProcessingResult, NotUsed] = {
    Flow[AirQualityReading]
      .map { reading =>
        val cleaned = AirQualityTransformations.cleanReading(reading)
        val aqi = cleaned.flatMap(AirQualityTransformations.calculateAQI)
        val alerts = cleaned.map(AlertRules.applyAllRules).getOrElse(Seq.empty)
        val validation = Validation.validateReading(reading)
        val validationErrors = validation.fold(_.toList, _ => List.empty)
        
        ProcessingResult(
          originalReading = reading,
          cleanedReading = cleaned,
          aqi = aqi,
          alerts = alerts,
          validationErrors = validationErrors
        )
      }
  }
  
  // Fenêtrage temporel pour agrégations
  def windowedAggregation: Flow[AirQualityReading, Statistics, NotUsed] = {
    Flow[AirQualityReading]
      .groupedWithin(100, 5.minutes)
      .map(readings => StatisticsCalculator.computeStatistics(readings, "5min"))
  }
  
  // Détection d'anomalies basée sur une fenêtre glissante
  def anomalyDetection: Flow[AirQualityReading, Seq[Alert], NotUsed] = {
    Flow[AirQualityReading]
      .sliding(10, 1)
      .map(detectAnomalies)
  }
  
  private def detectAnomalies(readings: Seq[AirQualityReading]): Seq[Alert] = {
    if (readings.length < 5) Seq.empty
    else {
      val pollutants = Seq(Pollutant.PM25, Pollutant.PM10, Pollutant.NO2, Pollutant.O3)
      pollutants.flatMap(pollutant => AlertRules.detectTrend(readings, pollutant))
    }
  }
  
  // Pipeline pour traitement par lot
  def batchProcessing: Flow[Seq[AirQualityReading], BatchResult, NotUsed] = {
    Flow[Seq[AirQualityReading]]
      .map { readings =>
        val processed = readings.map { reading =>
          val cleaned = AirQualityTransformations.cleanReading(reading)
          val aqi = cleaned.flatMap(AirQualityTransformations.calculateAQI)
          val alerts = cleaned.map(AlertRules.applyAllRules).getOrElse(Seq.empty)
          ProcessingResult(reading, cleaned, aqi, alerts)
        }
        
        val statistics = StatisticsCalculator.computeStatistics(readings, "batch")
        val allAlerts = processed.flatMap(_.alerts)
        
        BatchResult(
          processedReadings = processed,
          statistics = statistics,
          totalAlerts = allAlerts,
          processedCount = readings.length,
          validCount = processed.count(_.cleanedReading.isDefined)
        )
      }
  }
}

case class BatchResult(
  processedReadings: Seq[ProcessingResult],
  statistics: Statistics,
  totalAlerts: Seq[Alert],
  processedCount: Int,
  validCount: Int
)