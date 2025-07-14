// package com.airquality

// import java.time.Instant
// import scala.util.Random

// object Main extends App {
  
//   println("🌬️ Air Quality Monitor Démarré!")
//   println("=" * 50)
  
//   // Simulation simple
//   val random = new Random()
//   var counter = 0
  
//   while (true) {
//     try {
//       counter += 1
//       val pm25 = 10 + (random.nextGaussian() * 20).abs
//       val status = if (pm25 <= 35) "✅ Bon" else "⚠️ Dégradé"
      
//       println(f"$counter. PM2.5: $pm25%.1f μg/m³ - $status - ${Instant.now()}")
      
//       Thread.sleep(30000) // 30 secondes
      
//     } catch {
//       case e: Exception =>
//         println(s"❌ Erreur: ${e.getMessage}")
//         Thread.sleep(5000)
//     }
//   }
// }

package com.airquality

object Main {
  def main(args: Array[String]): Unit = {
    MainAdvanced.main(args)
  }
}