package com.airquality

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.airquality.application.AirQualityService
import com.airquality.domain.model._
import com.airquality.infrastructure.config.AppConfig
import com.typesafe.scalalogging.LazyLogging

import java.time.Instant
import scala.concurrent.duration._
import scala.util.Random

object Main extends App with LazyLogging {
  
  implicit val system: ActorSystem = ActorSystem("air-quality-system")
  implicit val ec = system.dispatcher
  
  logger.info("Démarrage du système de surveillance de la qualité de l'air")
  
  // Configuration
  val config = AppConfig.load()
  
  // Service principal
  val airQualityService = new AirQualityService()
  
  // Simulation de données
  val dataGenerator = Source.tick(0.seconds, 30.seconds, ())
    .map(_ => generateSimulatedReading())
    .via(airQualityService.processingFlow)
    .to(Sink.foreach { result =>
      logger.info(s"Résultat traité: $result")
    })
  
  // Démarrage du stream
  dataGenerator.run()
  
  logger.info("Système démarré. Appuyez sur Entrée pour arrêter...")
  scala.io.StdIn.readLine()
  
  system.terminate()
  
  private def generateSimulatedReading(): AirQualityReading = {
    val random = Random
    AirQualityReading(
      sensorId = s"sensor_${random.nextInt(10)}",
      timestamp = Instant.now(),
      location = Location(
        latitude = 48.8566 + (random.nextGaussian() * 0.1),
        longitude = 2.3522 + (random.nextGaussian() * 0.1)
      ),
      measurements = Map(
        Pollutant.PM25 -> 10 + (random.nextGaussian() * 20).abs,
        Pollutant.PM10 -> 15 + (random.nextGaussian() * 25).abs,
        Pollutant.NO2 -> 30 + (random.nextGaussian() * 40).abs,
        Pollutant.O3 -> 50 + (random.nextGaussian() * 30).abs
      ),
      metadata = Map(
        "source" -> "simulation",
        "version" -> "1.0"
      )
    )
  }
}