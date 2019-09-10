package com.sensorberg.executioner

import com.sensorberg.executioner.Executioner.POOL
import com.sensorberg.executioner.Executioner.SINGLE
import com.sensorberg.executioner.Executioner.UI
import com.sensorberg.executioner.Executioner.runOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

class ExecutionerTest {

    @Test
    fun `executioner runs on single thread`() {
        val waitForIt = CountDownLatch(1)
        val myThread = Thread.currentThread()
        var executionThread: Thread? = null
        runOn(SINGLE) {
            executionThread = Thread.currentThread()
            waitForIt.countDown()
        }
        waitForIt.await()
        assertNotEquals(myThread, executionThread)
    }

    @Test
    fun `executioner runs on pool threads`() {
        val waitForIt = CountDownLatch(1)
        val myThread = Thread.currentThread()
        var executionThread: Thread? = null
        runOn(POOL) {
            executionThread = Thread.currentThread()
            waitForIt.countDown()
        }
        waitForIt.await()
        assertNotEquals(myThread, executionThread)
    }

    @Test
    fun `executioner with delegate runs on same thread`() {
        Executioner.setDelegate(Executor { it.run() })
        val waitForIt = CountDownLatch(1)
        val myThread = Thread.currentThread()
        var executionThread: Thread? = null
        runOn(POOL) {
            executionThread = Thread.currentThread()
            waitForIt.countDown()
        }
        waitForIt.await()
        Executioner.setDelegate(null)
        assertEquals(myThread, executionThread)

    }

    @Test
    fun `runOn UI doesn't crash in unit test when delegate set`() {
        Executioner.setDelegate(Executor { it.run() })
        val waitForIt = CountDownLatch(1)
        val myThread = Thread.currentThread()
        var executionThread: Thread? = null
        runOn(UI) {
            executionThread = Thread.currentThread()
            waitForIt.countDown()
        }
        waitForIt.await()
        Executioner.setDelegate(null)
        assertEquals(myThread, executionThread)

    }

}
