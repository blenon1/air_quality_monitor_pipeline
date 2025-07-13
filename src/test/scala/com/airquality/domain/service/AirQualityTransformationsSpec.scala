package com.airquality.domain.service

import com.airquality.TestData
import com.airquality.domain.model._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AirQualityTransformationsSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  
  "calculateAQI" should "return a valid AQI for valid PM2.5 values" in {
    val reading = TestData.validAirQualityReading(pm25 = 25.0)
    val result = AirQualityTransformations.calculateAQI(reading)
    
    result shouldBe defined
    result.get.value should be > 0.0
    result.get.value should be < 500.0
    result.get.category shouldBe AQICategory.Moderate
  }
  
  it should "return None for readings without PM2.5" in {
    val reading = TestData.validAirQualityReading().copy(
      measurements = Map(Pollutant.NO2 -> 40.0)
    )
    val result = AirQualityTransformations.calculateAQI(reading)
    
    result shouldBe None
  }
  
  it should "categorize AQI correctly" in {
    val testCases = Seq(
      (5.0, AQICategory.Good),
      (25.0, AQICategory.Moderate),
      (45.0, AQICategory.UnhealthyForSensitive),
      (75.0, AQICategory.Unhealthy),
      (200.0, AQICategory.VeryUnhealthy),
      (350.0, AQICategory.Hazardous)
    )
    
    testCases.foreach { case (pm25Value, expectedCategory) =>
      val reading = TestData.validAirQualityReading(pm25 = pm25Value)
      val result = AirQualityTransformations.calculateAQI(reading)
      
      result shouldBe defined
      result.get.category shouldBe expectedCategory
    }
  }
  
  "cleanReading" should "accept valid readings" in {
    val reading = TestData.validAirQualityReading()
    val result = AirQualityTransformations.cleanReading(reading)
    
    result shouldBe defined
    result.get shouldBe reading
  }
  
  it should "reject readings with negative values" in {
    val reading = TestData.validAirQualityReading().copy(
      measurements = Map(Pollutant.PM25 -> -10.0)
    )
    val result = AirQualityTransformations.cleanReading(reading)
    
    result shouldBe None
  }
  
  it should "reject readings with extremely high values" in {
    val reading = TestData.validAirQualityReading().copy(
      measurements = Map(Pollutant.PM25 -> 1500.0)
    )
    val result = AirQualityTransformations.cleanReading(reading)
    
    result shouldBe None
  }
  
  it should "reject readings with empty sensor ID" in {
    val reading = TestData.validAirQualityReading().copy(sensorId = "")
    val result = AirQualityTransformations.cleanReading(reading)
    
    result shouldBe None
  }
  
  it should "filter out invalid measurements but keep valid ones" in {
    val reading = TestData.validAirQualityReading().copy(
      measurements = Map(
        Pollutant.PM25 -> 25.0,   // Valid
        Pollutant.PM10 -> -5.0,   // Invalid
        Pollutant.NO2 -> 1200.0   // Invalid
      )
    )
    val result = AirQualityTransformations.cleanReading(reading)
    
    result shouldBe defined
    result.get.measurements should contain only (Pollutant.PM25 -> 25.0)
  }
  
  "cleanReading" should "be idempotent" in {
    forAll { (pm25: Double, pm10: Double) =>
      whenever(pm25 >= 0 && pm25 < 1000 && pm10 >= 0 && pm10 < 1000) {
        val reading = TestData.validAirQualityReading(pm25 = pm25, pm10 = pm10)
        val cleaned = AirQualityTransformations.cleanReading(reading)
        val cleanedTwice = cleaned.flatMap(AirQualityTransformations.cleanReading)
        
        cleaned shouldEqual cleanedTwice
      }
    }
  }
  
  "enrichWithWeather" should "add weather metadata" in {
    val reading = TestData.validAirQualityReading()
    val enriched = AirQualityTransformations.enrichWithWeather(
      reading, 
      temperature = 22.5, 
      humidity = 65.0, 
      windSpeed = 3.2
    )
    
    enriched.metadata should contain ("temperature" -> "22.5")
    enriched.metadata should contain ("humidity" -> "65.0")
    enriched.metadata should contain ("windSpeed" -> "3.2")
    
    // Original metadata should be preserved
    enriched.metadata should contain ("source" -> "test")
    enriched.metadata should contain ("version" -> "1.0")
  }
  
  it should "preserve original reading data" in {
    val reading = TestData.validAirQualityReading()
    val enriched = AirQualityTransformations.enrichWithWeather(reading, 20.0, 60.0, 2.0)
    
    enriched.sensorId shouldBe reading.sensorId
    enriched.timestamp shouldBe reading.timestamp
    enriched.location shouldBe reading.location
    enriched.measurements shouldBe reading.measurements
  }
}