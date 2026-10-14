package com.dualprofile.app
import com.dualprofile.app.crossprofile.ReplayWindow
import org.junit.Assert.*
import org.junit.Test
class ReplayWindowTest {
 @Test(expected=IllegalArgumentException::class) fun replayRejectedAfterManyCommands() { var seen=mapOf("first" to 0L); repeat(300) { seen=ReplayWindow.accept("other$it",1,seen) }; ReplayWindow.accept("first",2,seen) }
 @Test fun expiresWithoutDroppingLiveRequests() { val seen=ReplayWindow.accept("new",400000,mapOf("old" to 0L,"live" to 399999L)); assertFalse(seen.containsKey("old")); assertTrue(seen.containsKey("live")) }
}
