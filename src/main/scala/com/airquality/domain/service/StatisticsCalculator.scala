package com.airquality.domain.service

import com.airquality.domain.model._
import java.time.Instant

case class PollutantStats(
  mean: Double,
  median: Double,
  min: Double,
  max: Double,
  stddev: Double,
  count: Int
)

case class Statistics(
  timestamp: Instant,
  sampleSize: Int,
  pollutantStats: Map[Pollutant, PollutantStats],
  timeWindow: String
)

object StatisticsCalculator {
  
  // Fonction pure pour calculer les statistiques
  def computeStatistics(readings: Seq[AirQualityReading], timeWindow: String = "5min"): Statistics = {
    val pollutantStats = readings
      .flatMap(_.measurements.toSeq)
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .mapValues(calculatePollutantStats)
      .toMap
      
    Statistics(
      timestamp = Instant.now(),
      sampleSize = readings.length,
      pollutantStats = pollutantStats,
      timeWindow = timeWindow
    )
  }
  
  private def calculatePollutantStats(values: Seq[Double]): PollutantStats = {
    if (values.isEmpty) {
      PollutantStats(0, 0, 0, 0, 0, 0)
    } else {
      val sorted = values.sorted
      PollutantStats(
        mean = values.sum / values.length,
        median = if (values.length % 2 == 0) {
          (sorted(values.length / 2 - 1) + sorted(values.length / 2)) / 2.0
        } else {
          sorted(values.length / 2)
        },
        min = values.min,
        max = values.max,
        stddev = calculateStdDev(values),
        count = values.length
      )
    }
  }
  
  // Fonction pure pour calculer la déviation standard
  def calculateStdDev(values: Seq[Double]): Double = {
    if (values.length <= 1) 0.0
    else {
      val mean = values.sum / values.length
      val variance = values.map(x => math.pow(x - mean, 2)).sum / (values.length - 1)
      math.sqrt(variance)
    }
  }
}