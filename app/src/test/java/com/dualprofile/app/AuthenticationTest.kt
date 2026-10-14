package com.dualprofile.app
import com.dualprofile.app.crossprofile.Authentication
import org.junit.Assert.*
import org.junit.Test
class AuthenticationTest {
 @Test fun tamperedCommandRejected() { val key=ByteArray(32){it.toByte()}; val s=Authentication.sign(key,"open:org.example"); assertTrue(Authentication.verify(key,"open:org.example",s)); assertFalse(Authentication.verify(key,"install:org.example",s)) }
 @Test fun otherPairingRejected() { assertFalse(Authentication.verify(ByteArray(32){2},"x",Authentication.sign(ByteArray(32){1},"x"))) }
 @Test fun expiredAndFutureRejected() { assertFalse(Authentication.fresh(0,300001)); assertFalse(Authentication.fresh(100000,0)); assertTrue(Authentication.fresh(100000,100000)) }
}
