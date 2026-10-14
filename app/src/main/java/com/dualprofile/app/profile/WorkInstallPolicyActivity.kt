package com.dualprofile.app.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dualprofile.app.data.DPLog
import com.dualprofile.app.installer.WorkInstaller

class WorkInstallPolicyActivity: ComponentActivity() {
 private lateinit var profiles: ProfileManager
 private var owner by mutableStateOf(false)
 private var ownRestriction by mutableStateOf(false)
 private var effectiveRestriction by mutableStateOf(true)
 private var sourceAllowed by mutableStateOf(false)
 private var message by mutableStateOf<String?>(null)
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  profiles=ProfileManager(this)
  refresh()
  setContent {
   var consentDialog by remember { mutableStateOf(false) }
   MaterialTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
    Text("Cài APK trong Work",style=MaterialTheme.typography.headlineSmall)
    Text("Cần hai bước: cho phép APK trong policy của Work và cấp quyền nguồn cài đặt cho Dual Profile trong Android.")
    Text("Dual Profile quản lý Work: ${if(owner) "Có" else "Không"}\nRestriction của Dual Profile: ${if(ownRestriction) "Đang chặn APK" else "Không chặn APK"}\nPolicy cài đặt hiệu lực: ${if(effectiveRestriction) "Đang chặn" else "Không chặn"}\nQuyền nguồn cài đặt Android: ${if(sourceAllowed) "Đã cấp" else "Chưa cấp"}")
    if(message!=null) Text(message!!)
    if(owner && ownRestriction) Button(onClick={consentDialog=true}) { Text("1. Cho phép APK custom trong Work") }
    if(owner && !ownRestriction && effectiveRestriction) Text("Một policy khác vẫn đang chặn. Dual Profile không thể gỡ policy của bên khác hoặc bảo vệ Samsung/Google.")
    if(owner) OutlinedButton(enabled=!effectiveRestriction,onClick={ openSourceSettings() }) { Text("2. Cấp quyền nguồn cài đặt Android") }
    Text("Không cần đăng nhập Google để copy APK đã cài ở Personal. APK, package name và chữ ký được giữ nguyên.")
    TextButton(onClick={ setResult(RESULT_OK,Intent().putExtra("message",if(!effectiveRestriction && sourceAllowed) "Quyền cài APK trong Work đã sẵn sàng. Refresh để chọn Copy APK to Work." else "Đã kiểm tra quyền Work. Hoàn tất hai bước trước khi cài APK custom.")); finish() }) { Text("Hoàn tất / quay lại") }
    if(consentDialog) AlertDialog(onDismissRequest={consentDialog=false},title={Text("Cho phép APK custom?")},text={Text("Dual Profile sẽ gỡ restriction cài nguồn ngoài do chính Dual Profile quản lý, chỉ trong Hồ sơ công việc. Không thay đổi Personal, Auto Blocker, Play Protect hoặc policy của bên khác. Bạn chỉ nên cài APK bạn tin tưởng; Android vẫn có thể yêu cầu xác nhận cài đặt.")},confirmButton={TextButton(onClick={ consentDialog=false; allowAfterConsent() }) { Text("Cho phép trong Work") }},dismissButton={TextButton(onClick={consentDialog=false}) { Text("Hủy") }})
   } } }
  }
 }
 override fun onResume() { super.onResume(); if(::profiles.isInitialized) refresh() }
 private fun refresh() {
  try {
   val snapshot=profiles.snapshot(); owner=snapshot.inWork && snapshot.owner
   ownRestriction=owner && profiles.dpm.getUserRestrictions(profiles.admin).getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
   effectiveRestriction=WorkInstaller.restrictions(this)
   sourceAllowed=packageManager.canRequestPackageInstalls()
  } catch(e: RuntimeException) { DPLog.failure("DPC",e); owner=false; effectiveRestriction=true; message="Không đọc được quyền Work. Không có policy nào được thay đổi." }
 }
 private fun allowAfterConsent() {
  try {
   val s=profiles.snapshot()
   val own=profiles.dpm.getUserRestrictions(profiles.admin).getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
   check(canChangeOwnWorkRestriction(s.owner,s.inWork,own,true))
   profiles.dpm.clearUserRestriction(profiles.admin,UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
   DPLog.info("DPC","User consent: removed only this admin's Work unknown-source restriction")
   refresh()
   message=if(effectiveRestriction) "Policy của Dual Profile đã được cập nhật nhưng một policy khác vẫn chặn. Không thay đổi policy đó." else "Đã cho phép APK trong policy Work. Tiếp theo, cấp quyền nguồn cài đặt qua Android nếu chưa được cấp."
  } catch(e: RuntimeException) { DPLog.failure("DPC",e); message="Không thể cập nhật restriction do Dual Profile quản lý. Không thay đổi policy của bên khác."; refresh() }
 }
 private fun openSourceSettings() {
  val intent=Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:$packageName"))
  if(intent.resolveActivity(packageManager)==null) { message="Android không cung cấp màn hình cấp quyền nguồn cài đặt trong Work."; return }
  try { startActivity(intent) } catch(e: RuntimeException) { DPLog.failure("Install",e); message="Android/OEM không cho mở màn hình này. Dual Profile không thể tự cấp quyền hoặc bỏ qua bảo vệ." }
 }
}
