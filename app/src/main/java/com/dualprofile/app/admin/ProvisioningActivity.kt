package com.dualprofile.app.admin
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import com.dualprofile.app.profile.ProfileManager
class ProvisioningActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  if(intent.action==DevicePolicyManager.ACTION_GET_PROVISIONING_MODE) {
   val allowed=intent.getIntegerArrayListExtra(DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES)
   if(allowed==null || allowed.contains(DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE)) setResult(RESULT_OK,Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE)) else setResult(RESULT_CANCELED)
  } else setResult(if(ProfileManager(this).initialize(intent)) RESULT_OK else RESULT_CANCELED)
  finish()
 }
}
