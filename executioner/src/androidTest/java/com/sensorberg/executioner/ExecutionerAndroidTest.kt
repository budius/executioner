package com.sensorberg.executioner

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExecutionerAndroidTest {

    @Test
    fun android_executioner_runs_on_main_thread() {
        val waitForIt = CountDownLatch(1)
        val androidUi = Executioner.AndroidUi()
        var looper: Looper? = null
        androidUi.execute {
            looper = Looper.myLooper()
            waitForIt.countDown()
        }
        assertTrue(waitForIt.await(100, TimeUnit.MILLISECONDS))
        assertEquals(looper, Looper.getMainLooper())
    }

    @Test
    fun android_executioner_runs_sequentially_when_already_on_main_thread() {
        val waitForIt = CountDownLatch(1)
        val androidUi = Executioner.AndroidUi()
        androidUi.execute {
            var executed = false
            androidUi.execute { executed = true }
            assertTrue(executed)
            waitForIt.countDown()
        }
        assertTrue(waitForIt.await(100, TimeUnit.MILLISECONDS))
    }

}
