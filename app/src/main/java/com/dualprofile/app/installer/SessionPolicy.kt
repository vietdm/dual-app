package com.dualprofile.app.installer

// This allows only a debug Probe attempt to reach the OS. It never grants installation rights.
fun sessionPreflightAllowsAttempt(debug: Boolean, packageName: String, diagnosticRequested: Boolean, installAppsBlocked: Boolean, unknownSourceBlocked: Boolean): Boolean {
 if(installAppsBlocked) return false
 return !unknownSourceBlocked || (debug && packageName=="com.dualprofile.probe" && diagnosticRequested)
}
