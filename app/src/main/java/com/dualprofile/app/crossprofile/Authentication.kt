package com.dualprofile.app.crossprofile
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
object Authentication {
 fun sign(key: ByteArray, payload: String): String = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key,"HmacSHA256")); doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) } }
 fun verify(key: ByteArray, payload: String, signature: String): Boolean = MessageDigest.isEqual(sign(key,payload).toByteArray(),signature.toByteArray())
 fun fresh(time: Long, now: Long) = time <= now + 30_000 && now - time <= 300_000
}
