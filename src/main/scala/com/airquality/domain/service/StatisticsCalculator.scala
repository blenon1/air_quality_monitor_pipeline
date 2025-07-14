package com.airquality.domain.service

import com.airquality.domain.model._
import java.time.Instant

object StatisticsCalculator {
  
  // Calcul de statistiques avec approche fonctionnelle
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
  
  // Calcul des statistiques pour un polluant
  private def calculatePollutantStats(values: Seq[Double]): PollutantStats = {
    if (values.isEmpty) {
      PollutantStats(0, 0, 0, 0, 0, 0)
    } else {
      val sorted = values.sorted
      val percentiles = Map(
        25 -> percentile(sorted, 0.25),
        50 -> percentile(sorted, 0.50),
        75 -> percentile(sorted, 0.75),
        90 -> percentile(sorted, 0.90),
        95 -> percentile(sorted, 0.95)
      )
      
      PollutantStats(
        mean = mean(values),
        median = median(sorted),
        min = values.min,
        max = values.max,
        stddev = standardDeviation(values),
        count = values.length,
        percentiles = percentiles
      )
    }
  }
  
  // Fonctions statistiques pures
  def mean(values: Seq[Double]): Double = 
    if (values.isEmpty) 0.0 else values.sum / values.length
  
  def median(sortedValues: Seq[Double]): Double = {
    if (sortedValues.isEmpty) 0.0
    else if (sortedValues.length % 2 == 0) {
      val mid = sortedValues.length / 2
      (sortedValues(mid - 1) + sortedValues(mid)) / 2.0
    } else {
      sortedValues(sortedValues.length / 2)
    }
  }
  
  def standardDeviation(values: Seq[Double]): Double = {
    if (values.length <= 1) 0.0
    else {
      val avg = mean(values)
      val variance = values.map(x => math.pow(x - avg, 2)).sum / (values.length - 1)
      math.sqrt(variance)
    }
  }
  
  def percentile(sortedValues: Seq[Double], p: Double): Double = {
    if (sortedValues.isEmpty) 0.0
    else {
      val index = (p * (sortedValues.length - 1)).toInt
      val fraction = (p * (sortedValues.length - 1)) - index
      
      if (index >= sortedValues.length - 1) sortedValues.last
      else sortedValues(index) + fraction * (sortedValues(index + 1) - sortedValues(index))
    }
  }
  
  // Détection d'outliers avec méthode IQR
  def detectOutliers(values: Seq[Double]): (Seq[Double], Seq[Double]) = {
    if (values.length < 4) (values, Seq.empty)
    else {
      val sorted = values.sorted
      val q1 = percentile(sorted, 0.25)
      val q3 = percentile(sorted, 0.75)
      val iqr = q3 - q1
      val lowerBound = q1 - 1.5 * iqr
      val upperBound = q3 + 1.5 * iqr
      
      values.partition(v => v >= lowerBound && v <= upperBound)
    }
  }
  
  // Analyse de tendances
  def calculateTrend(values: Seq[Double]): TrendDirection = {
    if (values.length < 2) TrendDirection.Stable
    else {
      val n = values.length
      val xValues = (1 to n).map(_.toDouble)
      val xMean = mean(xValues)
      val yMean = mean(values)
      
      val numerator = xValues.zip(values).map { case (x, y) => 
        (x - xMean) * (y - yMean) 
      }.sum
      
      val denominator = xValues.map(x => math.pow(x - xMean, 2)).sum
      
      val slope = if (denominator != 0) numerator / denominator else 0.0
      
      slope match {
        case s if s > 0.1 => TrendDirection.Increasing
        case s if s < -0.1 => TrendDirection.Decreasing
        case _ => TrendDirection.Stable
      }
    }
  }
  
  // Corrélation entre polluants
  def calculateCorrelation(values1: Seq[Double], values2: Seq[Double]): Double = {
    if (values1.length != values2.length || values1.length < 2) 0.0
    else {
      val mean1 = mean(values1)
      val mean2 = mean(values2)
      
      val numerator = values1.zip(values2).map { case (x, y) =>
        (x - mean1) * (y - mean2)
      }.sum
      
      val std1 = standardDeviation(values1)
      val std2 = standardDeviation(values2)
      
      if (std1 == 0.0 || std2 == 0.0) 0.0
      else numerator / ((values1.length - 1) * std1 * std2)
    }
  }
}

sealed trait TrendDirection
object TrendDirection {
  case object Increasing extends TrendDirection
  case object Decreasing extends TrendDirection
  case object Stable extends TrendDirection
}