package com.dualprofile.app.admin
import android.app.admin.DeviceAdminReceiver
import android.content.*
import com.dualprofile.app.profile.ProfileManager
class ProfileAdminReceiver: DeviceAdminReceiver() {
 override fun onProfileProvisioningComplete(context: Context,intent: Intent) { ProfileManager(context).initialize(intent) }
}
class ProfileEventsReceiver: BroadcastReceiver() {
 override fun onReceive(context: Context,intent: Intent) {
  // All listed actions are protected system broadcasts. Never infer success from this event alone.
  if(intent.action=="android.app.action.PROFILE_OWNER_CHANGED") ProfileManager(context).initialize()
 }
}
