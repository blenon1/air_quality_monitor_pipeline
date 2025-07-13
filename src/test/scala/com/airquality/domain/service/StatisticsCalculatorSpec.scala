package com.airquality.domain.service

import com.airquality.TestData
import com.airquality.domain.model.Pollutant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StatisticsCalculatorSpec extends AnyFlatSpec with Matchers {
  
  "computeStatistics" should "calculate correct statistics for sample data" in {
    val readings = TestData.sampleReadings(10)
    val stats = StatisticsCalculator.computeStatistics(readings)
    
    stats.sampleSize shouldBe 10
    stats.pollutantStats should contain key Pollutant.PM25
    stats.pollutantStats should contain key Pollutant.PM10
    stats.pollutantStats should contain key Pollutant.NO2
    
    val pm25Stats = stats.pollutantStats(Pollutant.PM25)
    pm25Stats.count shouldBe 10
    pm25Stats.mean should be > 0.0
    pm25Stats.min should be <= pm25Stats.max
  }
  
  it should "handle empty readings" in {
    val stats = StatisticsCalculator.computeStatistics(Seq.empty)
    
    stats.sampleSize shouldBe 0
    stats.pollutantStats shouldBe empty
  }
  
  it should "calculate median correctly for odd number of values" in {
    val values = Seq(1.0, 3.0, 5.0, 7.0, 9.0)
    val stats = StatisticsCalculator.calculatePollutantStats(values)
    
    stats.median shouldBe 5.0
  }
  
  it should "calculate median correctly for even number of values" in {
    val values = Seq(1.0, 3.0, 5.0, 7.0)
    val stats = StatisticsCalculator.calculatePollutantStats(values)
    
    stats.median shouldBe 4.0
  }
  
  "calculateStdDev" should "calculate standard deviation correctly" in {
    val values = Seq(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
    val stddev = StatisticsCalculator.calculateStdDev(values)
    
    stddev should be (2.0 +- 0.1)
  }
  
  it should "return 0 for single value" in {
    val stddev = StatisticsCalculator.calculateStdDev(Seq(5.0))
    stddev shouldBe 0.0
  }
  
  it should "return 0 for empty sequence" in {
    val stddev = StatisticsCalculator.calculateStdDev(Seq.empty)
    stddev shouldBe 0.0
  }
}