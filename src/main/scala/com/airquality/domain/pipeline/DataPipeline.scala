// package com.airquality.domain.pipeline

// import com.airquality.domain.model._
// import com.airquality.domain.service._

// // Types pour la composition monadique
// sealed trait DataResult[+A]
// case class Success[A](value: A) extends DataResult[A]
// case class Failure(errors: List[String]) extends DataResult[Nothing]

// object DataResult {
//   def apply[A](value: A): DataResult[A] = Success(value)
//   def fail(error: String): DataResult[Nothing] = Failure(List(error))
//   def fail(errors: List[String]): DataResult[Nothing] = Failure(errors)
  
//   implicit class DataResultOps[A](result: DataResult[A]) {
//     def map[B](f: A => B): DataResult[B] = result match {
//       case Success(value) => Success(f(value))
//       case Failure(errors) => Failure(errors)
//     }
    
//     def flatMap[B](f: A => DataResult[B]): DataResult[B] = result match {
//       case Success(value) => f(value)
//       case Failure(errors) => Failure(errors)
//     }
    
//     def filter(predicate: A => Boolean, error: String = "Filter failed"): DataResult[A] = result match {
//       case Success(value) if predicate(value) => Success(value)
//       case Success(_) => Failure(List(error))
//       case failure => failure
//     }
//   }
// }

// object DataPipeline {
  
//   // Pipeline de traitement principal - composition de fonctions pures
//   def processReading(reading: AirQualityReading): DataResult[ProcessingResult] = {
//     val startTime = System.currentTimeMillis()
    
//     for {
//       validated <- validateReading(reading)
//       cleaned <- cleanData(validated)
//       enriched <- enrichData(cleaned)
//       aqi <- calculateAQI(enriched)
//       alerts <- generateAlerts(enriched)
//     } yield ProcessingResult(
//       originalReading = reading,
//       cleanedReading = Some(enriched),
//       aqi = Some(aqi),
//       alerts = alerts,
//       processingTime = System.currentTimeMillis() - startTime
//     )
//   }
  
//   // Validation des données
//   private def validateReading(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     val errors = List.newBuilder[String]
    
//     // Validation du capteur
//     if (reading.sensorId.isEmpty || reading.sensorId.length > 50) {
//       errors += "ID capteur invalide"
//     }
    
//     // Validation des mesures
//     if (reading.measurements.isEmpty) {
//       errors += "Aucune mesure fournie"
//     }
    
//     reading.measurements.foreach { case (pollutant, value) =>
//       if (value < 0 || value > 1000 || value.isNaN || value.isInfinite) {
//         errors += s"Valeur invalide pour ${pollutant.name}: $value"
//       }
//     }
    
//     // Validation de la localisation
//     if (reading.location.latitude < -90 || reading.location.latitude > 90) {
//       errors += s"Latitude invalide: ${reading.location.latitude}"
//     }
//     if (reading.location.longitude < -180 || reading.location.longitude > 180) {
//       errors += s"Longitude invalide: ${reading.location.longitude}"
//     }
    
//     val errorsList = errors.result()
//     if (errorsList.isEmpty) DataResult(reading)
//     else DataResult.fail(errorsList)
//   }
  
//   // Nettoyage des données
//   private def cleanData(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     DataTransformations.cleanReading(reading) match {
//       case Some(cleaned) => DataResult(cleaned)
//       case None => DataResult.fail("Échec du nettoyage des données")
//     }
//   }
  
//   // Enrichissement des données
//   private def enrichData(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     // Simulation d'enrichissement avec données météo
//     val weatherContext = Map(
//       "temperature" -> (20.0 + scala.util.Random.nextGaussian() * 5),
//       "humidity" -> (60.0 + scala.util.Random.nextGaussian() * 10),
//       "windSpeed" -> (5.0 + scala.util.Random.nextGaussian() * 2).abs,
//       "pressure" -> (1013.0 + scala.util.Random.nextGaussian() * 10)
//     )
    
//     val enriched = DataTransformations.enrichWithContext(reading, weatherContext)
//     DataResult(enriched)
//   }
  
//   // Calcul de l'AQI
//   private def calculateAQI(reading: AirQualityReading): DataResult[AirQualityIndex] = {
//     DataTransformations.calculateAQI(reading) match {
//       case Some(aqi) => DataResult(aqi)
//       case None => DataResult.fail("Impossible de calculer l'AQI")
//     }
//   }
  
//   // Génération d'alertes
//   private def generateAlerts(reading: AirQualityReading): DataResult[List[Alert]] = {
//     val alerts = AlertEngine.applyAllRules(reading)
//     DataResult(alerts)
//   }
  
//   // Pipeline de traitement par lot avec composition fonctionnelle
//   def processBatch(readings: List[AirQualityReading]): BatchProcessingResult = {
//     val startTime = System.currentTimeMillis()
    
//     // Traitement parallèle fonctionnel
//     val results = readings.par.map(processReading).seq.toList
    
//     val (successes, failures) = results.partition {
//       case Success(_) => true
//       case Failure(_) => false
//     }
    
//     val successfulResults = successes.collect { case Success(result) => result }
//     val errors = failures.collect { case Failure(errs) => errs }.flatten
    
//     // Calcul des statistiques globales
//     val validReadings = successfulResults.flatMap(_.cleanedReading)
//     val statistics = if (validReadings.nonEmpty) {
//       Some(StatisticsCalculator.computeStatistics(validReadings, "batch"))
//     } else None
    
//     // Agrégation des alertes
//     val allAlerts = successfulResults.flatMap(_.alerts)
//     val prioritizedAlerts = AlertEngine.prioritizeAlerts(allAlerts)
//     val aggregatedAlerts = AlertEngine.aggregateSimilarAlerts(prioritizedAlerts)
    
//     BatchProcessingResult(
//       totalProcessed = readings.length,
//       successCount = successfulResults.length,
//       failureCount = failures.length,
//       processingTime = System.currentTimeMillis() - startTime,
//       results = successfulResults,
//       errors = errors,
//       statistics = statistics,
//       alerts = aggregatedAlerts
//     )
//   }
// }

// // Résultats de traitement
// case class BatchProcessingResult(
//   totalProcessed: Int,
//   successCount: Int,
//   failureCount: Int,
//   processingTime: Long,
//   results: List[ProcessingResult],
//   errors: List[String],
//   statistics: Option[Statistics],
//   alerts: List[Alert]
// )

// package com.airquality.domain.pipeline

// import com.airquality.domain.model._
// import com.airquality.domain.service._
// import scala.collection.parallel.CollectionConverters._

// // Types pour la composition monadique
// sealed trait DataResult[+A]
// case class Success[A](value: A) extends DataResult[A]
// case class Failure(errors: List[String]) extends DataResult[Nothing]

// object DataResult {
//   def apply[A](value: A): DataResult[A] = Success(value)
//   def fail(error: String): DataResult[Nothing] = Failure(List(error))
//   def fail(errors: List[String]): DataResult[Nothing] = Failure(errors)
  
//   implicit class DataResultOps[A](result: DataResult[A]) {
//     def map[B](f: A => B): DataResult[B] = result match {
//       case Success(value) => Success(f(value))
//       case Failure(errors) => Failure(errors)
//     }
    
//     def flatMap[B](f: A => DataResult[B]): DataResult[B] = result match {
//       case Success(value) => f(value)
//       case Failure(errors) => Failure(errors)
//     }
    
//     def filter(predicate: A => Boolean, error: String = "Filter failed"): DataResult[A] = result match {
//       case Success(value) if predicate(value) => Success(value)
//       case Success(_) => Failure(List(error))
//       case failure => failure
//     }
//   }
// }

// object DataPipeline {
  
//   // Pipeline de traitement principal - composition de fonctions pures
//   def processReading(reading: AirQualityReading): DataResult[ProcessingResult] = {
//     val startTime = System.currentTimeMillis()
    
//     for {
//       validated <- validateReading(reading)
//       cleaned <- cleanData(validated)
//       enriched <- enrichData(cleaned)
//       aqi <- calculateAQI(enriched)
//       alerts <- generateAlerts(enriched)
//     } yield ProcessingResult(
//       originalReading = reading,
//       cleanedReading = Some(enriched),
//       aqi = Some(aqi),
//       alerts = alerts,
//       processingTime = System.currentTimeMillis() - startTime
//     )
//   }
  
//   // Validation des données
//   private def validateReading(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     val errors = List.newBuilder[String]
    
//     // Validation du capteur
//     if (reading.sensorId.isEmpty || reading.sensorId.length > 50) {
//       errors += "ID capteur invalide"
//     }
    
//     // Validation des mesures
//     if (reading.measurements.isEmpty) {
//       errors += "Aucune mesure fournie"
//     }
    
//     reading.measurements.foreach { case (pollutant, value) =>
//       if (value < 0 || value > 1000 || value.isNaN || value.isInfinite) {
//         errors += s"Valeur invalide pour ${pollutant.name}: $value"
//       }
//     }
    
//     // Validation de la localisation
//     if (reading.location.latitude < -90 || reading.location.latitude > 90) {
//       errors += s"Latitude invalide: ${reading.location.latitude}"
//     }
//     if (reading.location.longitude < -180 || reading.location.longitude > 180) {
//       errors += s"Longitude invalide: ${reading.location.longitude}"
//     }
    
//     val errorsList = errors.result()
//     if (errorsList.isEmpty) DataResult(reading)
//     else DataResult.fail(errorsList)
//   }
  
//   // Nettoyage des données
//   private def cleanData(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     DataTransformations.cleanReading(reading) match {
//       case Some(cleaned) => DataResult(cleaned)
//       case None => DataResult.fail("Échec du nettoyage des données")
//     }
//   }
  
//   // Enrichissement des données
//   private def enrichData(reading: AirQualityReading): DataResult[AirQualityReading] = {
//     // Simulation d'enrichissement avec données météo
//     val weatherContext = Map(
//       "temperature" -> (20.0 + scala.util.Random.nextGaussian() * 5),
//       "humidity" -> (60.0 + scala.util.Random.nextGaussian() * 10),
//       "windSpeed" -> (5.0 + scala.util.Random.nextGaussian() * 2).abs,
//       "pressure" -> (1013.0 + scala.util.Random.nextGaussian() * 10)
//     )
    
//     val enriched = DataTransformations.enrichWithContext(reading, weatherContext)
//     DataResult(enriched)
//   }
  
//   // Calcul de l'AQI
//   private def calculateAQI(reading: AirQualityReading): DataResult[AirQualityIndex] = {
//     DataTransformations.calculateAQI(reading) match {
//       case Some(aqi) => DataResult(aqi)
//       case None => DataResult.fail("Impossible de calculer l'AQI")
//     }
//   }
  
//   // Génération d'alertes
//   private def generateAlerts(reading: AirQualityReading): DataResult[List[Alert]] = {
//     val alerts = AlertEngine.applyAllRules(reading)
//     DataResult(alerts)
//   }
  
//   // Pipeline de traitement par lot avec composition fonctionnelle
//   def processBatch(readings: List[AirQualityReading]): BatchProcessingResult = {
//     val startTime = System.currentTimeMillis()
    
//     // Traitement parallèle fonctionnel (version corrigée)
//     val results = readings.par.map(processReading).seq.toList
    
//     val (successResults, failureResults) = results.partition {
//       case Success(_) => true
//       case Failure(_) => false
//     }
    
//     val successfulResults = successResults.collect { case Success(result) => result }
//     val errors = failureResults.collect { case Failure(errs) => errs }.flatten
    
//     // Calcul des statistiques globales
//     val validReadings = successfulResults.flatMap(_.cleanedReading)
//     val statistics = if (validReadings.nonEmpty) {
//       Some(StatisticsCalculator.computeStatistics(validReadings, "batch"))
//     } else None
    
//     // Agrégation des alertes
//     val allAlerts = successfulResults.flatMap(_.alerts)
//     val prioritizedAlerts = AlertEngine.prioritizeAlerts(allAlerts)
//     val aggregatedAlerts = AlertEngine.aggregateSimilarAlerts(prioritizedAlerts)
    
//     BatchProcessingResult(
//       totalProcessed = readings.length,
//       successCount = successfulResults.length,
//       failureCount = failureResults.length,
//       processingTime = System.currentTimeMillis() - startTime,
//       results = successfulResults,
//       errors = errors,
//       statistics = statistics,
//       alerts = aggregatedAlerts
//     )
//   }
// }

// // Résultats de traitement
// case class BatchProcessingResult(
//   totalProcessed: Int,
//   successCount: Int,
//   failureCount: Int,
//   processingTime: Long,
//   results: List[ProcessingResult],
//   errors: List[String],
//   statistics: Option[Statistics],
//   alerts: List[Alert]
// )


package com.airquality.domain.pipeline

import com.airquality.domain.model._
import com.airquality.domain.service._

// Types pour la composition monadique
sealed trait DataResult[+A]
case class Success[A](value: A) extends DataResult[A]
case class Failure(errors: List[String]) extends DataResult[Nothing]

object DataResult {
  def apply[A](value: A): DataResult[A] = Success(value)
  def fail(error: String): DataResult[Nothing] = Failure(List(error))
  def fail(errors: List[String]): DataResult[Nothing] = Failure(errors)
  
  implicit class DataResultOps[A](result: DataResult[A]) {
    def map[B](f: A => B): DataResult[B] = result match {
      case Success(value) => Success(f(value))
      case Failure(errors) => Failure(errors)
    }
    
    def flatMap[B](f: A => DataResult[B]): DataResult[B] = result match {
      case Success(value) => f(value)
      case Failure(errors) => Failure(errors)
    }
    
    def filter(predicate: A => Boolean, error: String = "Filter failed"): DataResult[A] = result match {
      case Success(value) if predicate(value) => Success(value)
      case Success(_) => Failure(List(error))
      case failure => failure
    }
  }
}

object DataPipeline {
  
  // Pipeline de traitement principal - composition de fonctions pures
  def processReading(reading: AirQualityReading): DataResult[ProcessingResult] = {
    val startTime = System.currentTimeMillis()
    
    for {
      validated <- validateReading(reading)
      cleaned <- cleanData(validated)
      enriched <- enrichData(cleaned)
      aqi <- calculateAQI(enriched)
      alerts <- generateAlerts(enriched)
    } yield ProcessingResult(
      originalReading = reading,
      cleanedReading = Some(enriched),
      aqi = Some(aqi),
      alerts = alerts,
      processingTime = System.currentTimeMillis() - startTime
    )
  }
  
  // Validation des données
  private def validateReading(reading: AirQualityReading): DataResult[AirQualityReading] = {
    val errors = List.newBuilder[String]
    
    // Validation du capteur
    if (reading.sensorId.isEmpty || reading.sensorId.length > 50) {
      errors += "ID capteur invalide"
    }
    
    // Validation des mesures
    if (reading.measurements.isEmpty) {
      errors += "Aucune mesure fournie"
    }
    
    reading.measurements.foreach { case (pollutant, value) =>
      if (value < 0 || value > 1000 || value.isNaN || value.isInfinite) {
        errors += s"Valeur invalide pour ${pollutant.name}: $value"
      }
    }
    
    // Validation de la localisation
    if (reading.location.latitude < -90 || reading.location.latitude > 90) {
      errors += s"Latitude invalide: ${reading.location.latitude}"
    }
    if (reading.location.longitude < -180 || reading.location.longitude > 180) {
      errors += s"Longitude invalide: ${reading.location.longitude}"
    }
    
    val errorsList = errors.result()
    if (errorsList.isEmpty) DataResult(reading)
    else DataResult.fail(errorsList)
  }
  
  // Nettoyage des données
  private def cleanData(reading: AirQualityReading): DataResult[AirQualityReading] = {
    DataTransformations.cleanReading(reading) match {
      case Some(cleaned) => DataResult(cleaned)
      case None => DataResult.fail("Échec du nettoyage des données")
    }
  }
  
  // Enrichissement des données
  private def enrichData(reading: AirQualityReading): DataResult[AirQualityReading] = {
    // Simulation d'enrichissement avec données météo
    val weatherContext = Map(
      "temperature" -> (20.0 + scala.util.Random.nextGaussian() * 5),
      "humidity" -> (60.0 + scala.util.Random.nextGaussian() * 10),
      "windSpeed" -> (5.0 + scala.util.Random.nextGaussian() * 2).abs,
      "pressure" -> (1013.0 + scala.util.Random.nextGaussian() * 10)
    )
    
    val enriched = DataTransformations.enrichWithContext(reading, weatherContext)
    DataResult(enriched)
  }
  
  // Calcul de l'AQI
  private def calculateAQI(reading: AirQualityReading): DataResult[AirQualityIndex] = {
    DataTransformations.calculateAQI(reading) match {
      case Some(aqi) => DataResult(aqi)
      case None => DataResult.fail("Impossible de calculer l'AQI")
    }
  }
  
  // Génération d'alertes
  private def generateAlerts(reading: AirQualityReading): DataResult[List[Alert]] = {
    val alerts = AlertEngine.applyAllRules(reading)
    DataResult(alerts)
  }
  
  // Pipeline de traitement par lot (version séquentielle)
  def processBatch(readings: List[AirQualityReading]): BatchProcessingResult = {
    val startTime = System.currentTimeMillis()
    
    // Traitement séquentiel fonctionnel
    val results = readings.map(processReading)
    
    val (successResults, failureResults) = results.partition {
      case Success(_) => true
      case Failure(_) => false
    }
    
    val successfulResults = successResults.collect { case Success(result) => result }
    val errors = failureResults.collect { case Failure(errs) => errs }.flatten
    
    // Calcul des statistiques globales
    val validReadings = successfulResults.flatMap(_.cleanedReading)
    val statistics = if (validReadings.nonEmpty) {
      Some(StatisticsCalculator.computeStatistics(validReadings, "batch"))
    } else None
    
    // Agrégation des alertes
    val allAlerts = successfulResults.flatMap(_.alerts)
    val prioritizedAlerts = AlertEngine.prioritizeAlerts(allAlerts)
    val aggregatedAlerts = AlertEngine.aggregateSimilarAlerts(prioritizedAlerts)
    
    BatchProcessingResult(
      totalProcessed = readings.length,
      successCount = successfulResults.length,
      failureCount = failureResults.length,
      processingTime = System.currentTimeMillis() - startTime,
      results = successfulResults,
      errors = errors,
      statistics = statistics,
      alerts = aggregatedAlerts
    )
  }
}

// Résultats de traitement
case class BatchProcessingResult(
  totalProcessed: Int,
  successCount: Int,
  failureCount: Int,
  processingTime: Long,
  results: List[ProcessingResult],
  errors: List[String],
  statistics: Option[Statistics],
  alerts: List[Alert]
)