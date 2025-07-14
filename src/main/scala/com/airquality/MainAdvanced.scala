package com.airquality

import com.airquality.domain.model._
import com.airquality.domain.service._
import com.airquality.domain.pipeline._
import java.time.Instant
import scala.util.Random

object MainAdvanced extends App {
  
  println("🌬️ Air Quality Monitor - Traitements Fonctionnels Avancés")
  println("=" * 70)
  
  // Générateur de données simulées
  object DataGenerator {
    private val random = new Random()
    private val cities = List(
      ("Paris", 48.8566, 2.3522),
      ("Lyon", 45.7640, 4.8357),
      ("Marseille", 43.2965, 5.3698),
      ("Toulouse", 43.6047, 1.4442),
      ("Nice", 43.7102, 7.2620)
    )
    
    def generateReading(sensorId: String = s"sensor-${random.nextInt(100)}"): AirQualityReading = {
      val (city, lat, lon) = cities(random.nextInt(cities.length))
      val baseTime = Instant.now()
      
      // Simulation de données réalistes avec corrélations
      val basePM25 = 15 + random.nextGaussian() * 10
      val pm25 = math.max(0, basePM25 + (if (random.nextDouble() < 0.1) random.nextGaussian() * 50 else 0))
      val pm10 = math.max(0, pm25 * 1.5 + random.nextGaussian() * 5)
      val no2 = math.max(0, 30 + random.nextGaussian() * 20 + (if (city == "Paris") 20 else 0))
      val o3 = math.max(0, 80 + random.nextGaussian() * 30 - pm25 * 0.3) // Anti-corrélation avec PM2.5
      val co = math.max(0, 2 + random.nextGaussian() * 1)
      
      AirQualityReading(
        sensorId = sensorId,
        timestamp = baseTime.minusSeconds(random.nextInt(300)),
        location = Location(
          latitude = lat + random.nextGaussian() * 0.01,
          longitude = lon + random.nextGaussian() * 0.01,
          city = Some(city)
        ),
        measurements = Map(
          Pollutant.PM25 -> pm25,
          Pollutant.PM10 -> pm10,
          Pollutant.NO2 -> no2,
          Pollutant.O3 -> o3,
          Pollutant.CO -> co
        ),
        metadata = Map(
          "source" -> "simulation",
          "city" -> city,
          "sensor_type" -> (if (random.nextBoolean()) "reference" else "low_cost")
        )
      )
    }
    
    def generateBatch(size: Int): List[AirQualityReading] = {
      (1 to size).map(_ => generateReading()).toList
    }
  }
  
  // Démonstration des traitements
  def demonstrateProcessing(): Unit = {
    println("\n🔄 DÉMONSTRATION DES TRAITEMENTS FONCTIONNELS")
    println("-" * 50)
    
    // 1. Traitement d'une lecture unique
    println("\n1. Traitement d'une lecture unique:")
    val singleReading = DataGenerator.generateReading("demo-sensor-001")
    println(s"   Lecture originale: ${formatReading(singleReading)}")
    
    DataPipeline.processReading(singleReading) match {
      case Success(result) =>
        println(s"   ✅ Traitement réussi en ${result.processingTime}ms")
        result.aqi.foreach(aqi => println(s"   📊 AQI: ${aqi.value.formatted("%.1f")} (${aqi.category.name})"))
        if (result.alerts.nonEmpty) {
          println(s"   🚨 ${result.alerts.length} alerte(s) générée(s):")
          result.alerts.foreach(alert => println(s"      - ${alert.severity.name}: ${alert.message}"))
        }
      case Failure(errors) =>
        println(s"   ❌ Échec du traitement: ${errors.mkString(", ")}")
    }
    
    // 2. Traitement par lot
    println("\n2. Traitement par lot (100 lectures):")
    val batch = DataGenerator.generateBatch(100)
    val batchResult = DataPipeline.processBatch(batch)
    
    println(s"   📦 Traité: ${batchResult.totalProcessed} lectures")
    println(s"   ✅ Succès: ${batchResult.successCount}")
    println(s"   ❌ Échecs: ${batchResult.failureCount}")
    println(s"   ⏱️  Temps: ${batchResult.processingTime}ms")
    
    batchResult.statistics.foreach { stats =>
      println(s"   📊 Statistiques globales:")
      stats.pollutantStats.foreach { case (pollutant, pollutantStats) =>
        println(f"      ${pollutant.name}: ${pollutantStats.mean}%.1f ± ${pollutantStats.stddev}%.1f ${pollutant.unit}")
      }
    }
    
    if (batchResult.alerts.nonEmpty) {
      println(s"   🚨 ${batchResult.alerts.length} alerte(s) prioritaire(s):")
      batchResult.alerts.take(5).foreach { alert =>
        println(s"      - ${alert.severity.name}: ${alert.message}")
      }
    }
    
    // 3. Calculs de corrélation
    println("\n3. Analyse des corrélations entre polluants:")
    analyzeCorrelations(batch)
  }
  
  def analyzeCorrelations(readings: List[AirQualityReading]): Unit = {
    val pollutantPairs = for {
      p1 <- Pollutant.all
      p2 <- Pollutant.all
      if p1 != p2
    } yield (p1, p2)
    
    pollutantPairs.foreach { case (p1, p2) =>
      val values1 = readings.flatMap(_.measurements.get(p1))
      val values2 = readings.flatMap(_.measurements.get(p2))
      
      if (values1.length == values2.length && values1.length > 10) {
        val correlation = StatisticsCalculator.calculateCorrelation(values1, values2)
        if (math.abs(correlation) > 0.3) {
          val corrType = if (correlation > 0) "positive" else "négative"
          println(f"   🔗 ${p1.name} ↔ ${p2.name}: corrélation $corrType (${correlation}%.3f)")
        }
      }
    }
  }
  
  def formatReading(reading: AirQualityReading): String = {
    val mainPollutants = reading.measurements.map { case (p, v) => s"${p.name}:${v.formatted("%.1f")}" }.mkString(", ")
    s"${reading.sensorId} @${reading.location.city.getOrElse("?")} [$mainPollutants]"
  }
  
  // Simulation en temps réel
  def runRealTimeSimulation(): Unit = {
    println("\n🔄 SIMULATION TEMPS RÉEL")
    println("-" * 30)
    println("Génération d'une lecture toutes les 30 secondes...")
    
    var counter = 0
    
    while (true) {
      try {
        counter += 1
        val reading = DataGenerator.generateReading(s"realtime-sensor-${counter % 3}")
        
        // Traitement de la lecture
        DataPipeline.processReading(reading) match {
          case Success(result) =>
            val status = result.aqi.map(_.category.name).getOrElse("Inconnu")
            val alertCount = result.alerts.length
            val alertIndicator = if (alertCount > 0) s"🚨×$alertCount" else "✅"
            
            println(f"$counter%3d. ${result.originalReading.timestamp} $alertIndicator $status - ${formatReading(reading)}")
            
            // Afficher les alertes critiques
            result.alerts.filter(_.severity.level >= 3).foreach { alert =>
              println(s"     ⚠️  ${alert.message}")
            }
            
          case Failure(errors) =>
            println(f"$counter%3d. ❌ Erreur: ${errors.mkString(", ")}")
        }
        
        Thread.sleep(30000) // 30 secondes
        
      } catch {
        case _: InterruptedException =>
          println("\n🛑 Simulation arrêtée")
          return
        case e: Exception =>
          println(s"❌ Erreur: ${e.getMessage}")
          Thread.sleep(5000)
      }
    }
  }
  
  // Exécution
  try {
    // Démonstration automatique
    demonstrateProcessing()
    
    // Puis simulation temps réel
    runRealTimeSimulation()
    
  } catch {
    case e: Exception =>
      println(s"❌ Erreur fatale: ${e.getMessage}")
      e.printStackTrace()
  }
}