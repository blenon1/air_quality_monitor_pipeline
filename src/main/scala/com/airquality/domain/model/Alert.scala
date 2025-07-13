package com.airquality.domain.model

import java.time.Instant
import java.util.UUID

case class Alert(
  id: String = UUID.randomUUID().toString,
  severity: AlertSeverity,
  message: String,
  location: Location,
  timestamp: Instant,
  pollutant: Option[Pollutant] = None,
  value: Option[Double] = None
)

sealed trait AlertSeverity {
  def level: Int
  def name: String
}

object AlertSeverity {
  case object Info extends AlertSeverity {
    val level = 1
    val name = "Info"
  }
  
  case object Warning extends AlertSeverity {
    val level = 2
    val name = "Warning"
  }
  
  case object Critical extends AlertSeverity {
    val level = 3
    val name = "Critical"
  }
  
  case object Emergency extends AlertSeverity {
    val level = 4
    val name = "Emergency"
  }
}