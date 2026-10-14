package com.dualprofile.probe
import android.app.*
import android.content.*
import android.os.*
import android.widget.*
class ProbeActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("probe",MODE_PRIVATE)
  val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(36,60,36,36) }
  val title=TextView(this).apply { text="Dual Profile Probe\nProfile: ${if((Build.VERSION.SDK_INT>=30 && getSystemService(UserManager::class.java).isManagedProfile)) "Work / B" else "Personal / A"}"; textSize=22f }
  val input=EditText(this).apply { hint="Save A in Personal; B in Work"; setText(prefs.getString("value","")) }
  val saved=TextView(this).apply { text="Saved value: ${prefs.getString("value", "empty")}" }
  layout.addView(title); layout.addView(input); layout.addView(saved)
  layout.addView(Button(this).apply { text="Save independent value"; setOnClickListener { prefs.edit().putString("value",input.text.toString()).apply(); saved.text="Saved value: ${input.text}" } })
  layout.addView(Button(this).apply { text="Notification in about 60 seconds"; setOnClickListener {
   if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),1)
   val pending=PendingIntent.getBroadcast(this@ProbeActivity,1,Intent(this@ProbeActivity,AlarmReceiver::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
   getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+60000,pending)
   Toast.makeText(this@ProbeActivity,"Alarm scheduled. Android may delay it. Kill Dual Profile, keep Probe enabled.",Toast.LENGTH_LONG).show()
  } })
  setContentView(layout)
 }
}
class AlarmReceiver: BroadcastReceiver() {
 override fun onReceive(context: Context,intent: Intent) {
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("probe","Independent profile probe",NotificationManager.IMPORTANCE_DEFAULT))
  if(Build.VERSION.SDK_INT<33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED) manager.notify(1,Notification.Builder(context,"probe").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Dual Profile Probe").setContentText("Saved in this profile: ${context.getSharedPreferences("probe",Context.MODE_PRIVATE).getString("value","empty")}").build())
 }
}
