package com.airquality.domain.pipeline

import cats.data.Kleisli
import cats.implicits._
import com.airquality.domain.model._
import com.airquality.domain.service._
import com.airquality.application.ProcessingResult

object PipelineComposition {
  
  // Types pour la composition monadique
  type DataTransform[A, B] = Kleisli[Option, A, B]
  
  // Transformation de base : nettoyage
  val cleaningTransform: DataTransform[AirQualityReading, AirQualityReading] = 
    Kleisli(AirQualityTransformations.cleanReading)
  
  // Transformation : calcul AQI
  val aqiTransform: DataTransform[AirQualityReading, AirQualityIndex] = 
    Kleisli(AirQualityTransformations.calculateAQI)
  
  // Composition de transformations
  val dataProcessingPipeline: AirQualityReading => Option[AirQualityIndex] = {
    (cleaningTransform andThen aqiTransform).run
  }
  
  // Pipeline complet avec gestion d'erreurs
  def fullProcessingPipeline(reading: AirQualityReading): ProcessingResult = {
    val validationResult = Validation.validateReading(reading)
    val validationErrors = validationResult.fold(_.toList, _ => List.empty)
    
    val processedData = for {
      cleaned <- AirQualityTransformations.cleanReading(reading)
      aqi <- AirQualityTransformations.calculateAQI(cleaned)
    } yield (cleaned, aqi)
    
    val alerts = processedData match {
      case Some((cleaned, _)) => AlertRules.applyAllRules(cleaned)
      case None => Seq.empty
    }
    
    ProcessingResult(
      originalReading = reading,
      cleanedReading = processedData.map(_._1),
      aqi = processedData.map(_._2),
      alerts = alerts,
      validationErrors = validationErrors
    )
  }
  
  // Composition fonctionnelle avec enrichissement
  def enrichedProcessingPipeline(
    reading: AirQualityReading,
    weatherData: Option[WeatherData] = None
  ): ProcessingResult = {
    
    val enrichedReading = weatherData match {
      case Some(weather) => 
        AirQualityTransformations.enrichWithWeather(
          reading, 
          weather.temperature, 
          weather.humidity, 
          weather.windSpeed
        )
      case None => reading
    }
    
    fullProcessingPipeline(enrichedReading)
  }
}

case class WeatherData(
  temperature: Double,
  humidity: Double,
  windSpeed: Double,
  pressure: Double
)