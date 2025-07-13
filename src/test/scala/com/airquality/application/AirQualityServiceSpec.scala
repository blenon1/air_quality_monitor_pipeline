package com.airquality.application

import com.airquality.TestData
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.ExecutionContext.Implicits.global

class AirQualityServiceSpec extends AnyFlatSpec with Matchers {
  
  val service = new AirQualityService()
  
  "processReading" should "process valid reading successfully" in {
    val reading = TestData.validAirQualityReading()
    val result = service.processReading(reading)
    
    result.originalReading shouldBe reading
    result.cleanedReading shouldBe defined
    result.aqi shouldBe defined
    result.validationErrors shouldBe empty
  }
  
  it should "handle invalid reading" in {
    val reading = TestData.invalidAirQualityReading()
    val result = service.processReading(reading)
    
    result.originalReading shouldBe reading
    result.cleanedReading shouldBe empty
    result.aqi shouldBe empty
    result.validationErrors should not be empty
  }
  
  it should "generate alerts for critical readings" in {
    val reading = TestData.criticalPollutionReading()
    val result = service.processReading(reading)
    
    result.alerts should not be empty
    result.alerts.exists(_.severity == com.airquality.domain.model.AlertSeverity.Emergency) shouldBe true
  }
  
  "generateStatistics" should "produce statistics for multiple readings" in {
    val readings = TestData.sampleReadings(50)
    val stats = service.generateStatistics(readings)
    
    stats.sampleSize shouldBe 50
    stats.pollutantStats should not be empty
  }
}