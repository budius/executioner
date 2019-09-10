package com.sensorberg.executioner

import com.sensorberg.executioner.Executioner.runOn
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WarningTest {
    @Test
    fun executing_on_normal_executor_creates_warning() {
        val didHappen = AtomicBoolean(false)
        val waitForIt = CountDownLatch(1)
        val executor = Executor { it.run() }
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (priority == android.util.Log.WARN &&
                    message == Executioner.notWrappedExecutorWarningMessage
                ) {
                    didHappen.set(true)
                }
            }
        }
        Timber.plant(tree)
        runOn(executor) {
            waitForIt.countDown()
        }
        assertTrue(waitForIt.await(100, TimeUnit.MILLISECONDS))
        assertTrue(didHappen.get())
        Timber.uproot(tree)
    }
}
