package com.dualprofile.app
import com.dualprofile.app.installer.sessionPreflightAllowsAttempt
import org.junit.Assert.*
import org.junit.Test
class SessionPolicyTest {
 @Test fun productionCustomApkCannotOptIntoDiagnostic() { assertFalse(sessionPreflightAllowsAttempt(false,"org.example.custom",true,false,true)) }
 @Test fun diagnosticIsLimitedToLawfulProbe() { assertFalse(sessionPreflightAllowsAttempt(true,"org.example.custom",true,false,true)); assertTrue(sessionPreflightAllowsAttempt(true,"com.dualprofile.probe",true,false,true)) }
 @Test fun installAppsPolicyStillStopsProbeAttempt() { assertFalse(sessionPreflightAllowsAttempt(true,"com.dualprofile.probe",true,true,true)) }
 @Test fun normalAllowedSourceCanReachInstaller() { assertTrue(sessionPreflightAllowsAttempt(false,"org.example.custom",false,false,false)) }
}
