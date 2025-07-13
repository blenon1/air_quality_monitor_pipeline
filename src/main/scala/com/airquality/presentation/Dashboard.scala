package com.airquality.presentation

import com.airquality.domain.model._
import com.airquality.application.ProcessingResult
import com.airquality.infrastructure.metrics.ProcessingMetrics

object Dashboard {
  
  // Génération d'un dashboard textuel simple
  def generateTextDashboard(
    recentResults: Seq[ProcessingResult],
    metrics: ProcessingMetrics,
    alerts: Seq[Alert]
  ): String = {
    val sb = new StringBuilder
    
    sb.append("═" * 80 + "\n")
    sb.append("   🌬️  TABLEAU DE BORD QUALITÉ DE L'AIR  🌬️\n")
    sb.append("═" * 80 + "\n\n")
    
    // Section métriques
    sb.append("📊 MÉTRIQUES TEMPS RÉEL\n")
    sb.append("─" * 40 + "\n")
    sb.append(f"Lectures traitées    : ${metrics.readingsProcessed}%8d\n")
    sb.append(f"Alertes générées     : ${metrics.alertsGenerated}%8d\n")
    sb.append(f"Erreurs              : ${metrics.errors}%8d\n")
    sb.append(f"Temps moyen          : ${metrics.averageProcessingTime.toMillis}%6dms\n")
    sb.append("\n")
    
    // Section alertes actives
    val activeAlerts = alerts.filter(_.timestamp.isAfter(java.time.Instant.now().minusSeconds(3600)))
    sb.append(s"🚨 ALERTES ACTIVES (${activeAlerts.length})\n")
    sb.append("─" * 40 + "\n")
    
    if (activeAlerts.isEmpty) {
      sb.append("   ✅ Aucune alerte active\n")
    } else {
      activeAlerts.take(5).foreach { alert =>
        val severityIcon = alert.severity match {
          case AlertSeverity.Emergency => "🔴"
          case AlertSeverity.Critical => "🟠"
          case AlertSeverity.Warning => "🟡"
          case AlertSeverity.Info => "🔵"
        }
        sb.append(f"   $severityIcon ${alert.severity.name}%-10s: ${alert.message}\n")
      }
      if (activeAlerts.length > 5) {
        sb.append(f"   ... et ${activeAlerts.length - 5} autres alertes\n")
      }
    }
    sb.append("\n")
    
    // Section qualité de l'air actuelle
    val latestResults = recentResults.take(5)
    sb.append("🌡️ QUALITÉ DE L'AIR RÉCENTE\n")
    sb.append("─" * 40 + "\n")
    
    if (latestResults.isEmpty) {
      sb.append("   📭 Aucune donnée récente\n")
    } else {
      latestResults.foreach { result =>
        result.aqi match {
          case Some(aqi) =>
            val categoryIcon = aqi.category match {
              case AQICategory.Good => "🟢"
              case AQICategory.Moderate => "🟡"
              case AQICategory.UnhealthyForSensitive => "🟠"
              case AQICategory.Unhealthy => "🔴"
              case AQICategory.VeryUnhealthy => "🟣"
              case AQICategory.Hazardous => "⚫"
            }
            sb.append(f"   $categoryIcon ${result.originalReading.sensorId}%-15s AQI: ${aqi.value}%6.1f (${aqi.category.name})\n")
          case None =>
            sb.append(f"   ❌ ${result.originalReading.sensorId}%-15s Données invalides\n")
        }
      }
    }
    sb.append("\n")
    
    // Section statistiques rapides
    if (recentResults.nonEmpty) {
      val validResults = recentResults.filter(_.aqi.isDefined)
      if (validResults.nonEmpty) {
        val avgAQI = validResults.map(_.aqi.get.value).sum / validResults.length
        val maxAQI = validResults.map(_.aqi.get.value).max
        val minAQI = validResults.map(_.aqi.get.value).min
        
        sb.append("📈 STATISTIQUES RAPIDES\n")
        sb.append("─" * 40 + "\n")
        sb.append(f"AQI moyen            : ${avgAQI}%6.1f\n")
        sb.append(f"AQI min/max          : ${minAQI}%6.1f / ${maxAQI}%6.1f\n")
        sb.append(f"Lectures valides     : ${validResults.length}%3d / ${recentResults.length}\n")
        sb.append("\n")
      }
    }
    
    // Section recommandations
    sb.append("💡 RECOMMANDATIONS\n")
    sb.append("─" * 40 + "\n")
    val criticalAlerts = activeAlerts.count(_.severity == AlertSeverity.Critical)
    val emergencyAlerts = activeAlerts.count(_.severity == AlertSeverity.Emergency)
    
    if (emergencyAlerts > 0) {
      sb.append("   🚨 SITUATION D'URGENCE\n")
      sb.append("      • Évitez toute activité extérieure\n")
      sb.append("      • Fermez fenêtres et aérations\n")
      sb.append("      • Consultez un médecin si symptômes\n")
    } else if (criticalAlerts > 0) {
      sb.append("   ⚠️  POLLUTION ÉLEVÉE\n")
      sb.append("      • Limitez les activités extérieures\n")
      sb.append("      • Portez un masque si nécessaire\n")
      sb.append("      • Surveillez la qualité de l'air\n")
    } else {
      sb.append("   ✅ Conditions normales\n")
      sb.append("      • Qualité de l'air acceptable\n")
      sb.append("      • Activités normales possibles\n")
    }
    
    sb.append("\n")
    sb.append("─" * 80 + "\n")
    sb.append(s"Dernière mise à jour: ${java.time.Instant.now()}\n")
    sb.append("═" * 80 + "\n")
    
    sb.toString()
  }
  
  // Génération d'un résumé compact
  def generateCompactSummary(
    recentResults: Seq[ProcessingResult],
    alerts: Seq[Alert]
  ): String = {
    val validResults = recentResults.filter(_.aqi.isDefined)
    val avgAQI = if (validResults.nonEmpty) {
      Some(validResults.map(_.aqi.get.value).sum / validResults.length)
    } else None
    
    val emergencyAlerts = alerts.count(_.severity == AlertSeverity.Emergency)
    val criticalAlerts = alerts.count(_.severity == AlertSeverity.Critical)
    
    val status = if (emergencyAlerts > 0) "🚨 URGENCE"
                else if (criticalAlerts > 0) "⚠️ CRITIQUE" 
                else "✅ NORMAL"
    
    avgAQI match {
      case Some(aqi) => f"$status | AQI: $aqi%.1f | Alertes: ${alerts.length} | Lectures: ${recentResults.length}"
      case None => f"$status | Pas de données AQI | Alertes: ${alerts.length} | Lectures: ${recentResults.length}"
    }
  }
}