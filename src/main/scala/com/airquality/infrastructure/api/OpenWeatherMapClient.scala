package com.airquality.infrastructure.api

import com.airquality.domain.model._
import io.circe.{Decoder, HCursor}
import io.circe.generic.auto._
import io.circe.parser._
import sttp.client3._
import sttp.client3.circe._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class OpenWeatherResponse(
  coord: Coordinates,
  list: List[AirPollutionData]
)

case class Coordinates(lat: Double, lon: Double)

case class AirPollutionData(
  dt: Long,
  main: AirQualityMain,
  components: Map[String, Double]
)

case class AirQualityMain(aqi: Int)

class OpenWeatherMapClient(apiKey: String)(implicit ec: ExecutionContext) {
  
  private val backend = HttpURLConnectionBackend()
  private val baseUrl = "http://api.openweathermap.org/data/2.5/air_pollution"
  
  def getCurrentAirQuality(location: Location): Future[Option[AirQualityReading]] = {
    val url = s"$baseUrl/current?lat=${location.latitude}&lon=${location.longitude}&appid=$apiKey"
    
    val request = basicRequest
      .get(uri"$url")
      .response(asJson[OpenWeatherResponse])
    
    Future {
      request.send(backend) match {
        case Response(Right(response), _, _, _, _, _) =>
          convertToAirQualityReading(response, location)
        case Response(Left(error), _, _, _, _, _) =>
          println(s"Erreur API: $error")
          None
        case response =>
          println(s"Réponse inattendue: $response")
          None
      }
    }
  }
  
  private def convertToAirQualityReading(
    response: OpenWeatherResponse, 
    location: Location
  ): Option[AirQualityReading] = {
    response.list.headOption.map { data =>
      val measurements = Map(
        Pollutant.PM25 -> data.components.getOrElse("pm2_5", 0.0),
        Pollutant.PM10 -> data.components.getOrElse("pm10", 0.0),
        Pollutant.NO2 -> data.components.getOrElse("no2", 0.0),
        Pollutant.O3 -> data.components.getOrElse("o3", 0.0),
        Pollutant.CO -> data.components.getOrElse("co", 0.0)
      ).filter(_._2 > 0.0)
      
      AirQualityReading(
        sensorId = s"openweather-${location.latitude}-${location.longitude}",
        timestamp = Instant.ofEpochSecond(data.dt),
        location = location,
        measurements = measurements,
        metadata = Map(
          "source" -> "openweathermap",
          "aqi" -> data.main.aqi.toString
        )
      )
    }
  }
}