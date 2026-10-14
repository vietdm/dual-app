package com.dualprofile.app.crossprofile
object ReplayWindow {
 fun accept(nonce: String, now: Long, previous: Map<String,Long>): Map<String,Long> {
  val live=previous.filterValues { now-it<=330_000 }.toMutableMap()
  require(nonce !in live) { "Replayed request" }
  require(live.size<2048) { "Too many requests; try again later" }
  live[nonce]=now
  return live
 }
}
