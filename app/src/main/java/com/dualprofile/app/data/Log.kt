package com.dualprofile.app.data
import android.util.Log
object DPLog {
 fun info(area: String, message: String) { Log.i("DualProfile-$area", message) }
 fun failure(area: String, error: Throwable) { Log.e("DualProfile-$area", error.javaClass.simpleName + ": " + error.message) }
}
