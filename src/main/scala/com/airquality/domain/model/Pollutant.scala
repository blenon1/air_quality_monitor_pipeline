package com.airquality.domain.model

sealed trait Pollutant {
  def name: String
  def unit: String
}

object Pollutant {
  case object PM25 extends Pollutant {
    val name = "PM2.5"
    val unit = "μg/m³"
  }
  
  case object PM10 extends Pollutant {
    val name = "PM10"
    val unit = "μg/m³"
  }
  
  case object NO2 extends Pollutant {
    val name = "NO2"
    val unit = "μg/m³"
  }
  
  case object O3 extends Pollutant {
    val name = "O3"
    val unit = "μg/m³"
  }
  
  case object CO extends Pollutant {
    val name = "CO"
    val unit = "mg/m³"
  }
  
  val all: List[Pollutant] = List(PM25, PM10, NO2, O3, CO)
}