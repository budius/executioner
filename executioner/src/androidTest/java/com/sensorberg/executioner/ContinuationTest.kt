package com.sensorberg.executioner

import com.sensorberg.executioner.Executioner.POOL
import com.sensorberg.executioner.Executioner.SINGLE
import com.sensorberg.executioner.Executioner.UI
import com.sensorberg.executioner.Executioner.runOn
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class ContinuationTest {

    /**
     * runs 4 iterations on UI->POOL->SINGLE->UI
     */
    @Test
    fun continuation_tests_simple_1() {
        val preparation = CountDownLatch(2)
        val threads = mutableMapOf<Executor, Thread>()
        runOn(UI) {
            synchronized(threads) { threads[UI] = Thread.currentThread() }
            preparation.countDown()
        }
        runOn(SINGLE) {
            synchronized(threads) { threads[SINGLE] = Thread.currentThread() }
            preparation.countDown()
        }
        assertTrue(preparation.await(100, TimeUnit.MILLISECONDS))

        // test
        val waitForIt = CountDownLatch(4)
        val atomicInteger = AtomicInteger(0)
        runOn(UI) {
            assertEquals(1, atomicInteger.addAndGet(1))
            assertEquals(threads[UI], Thread.currentThread())
            waitForIt.countDown()
        }.andThen(POOL) {
            assertEquals(2, atomicInteger.addAndGet(1))
            assertNull(threads[POOL])
            waitForIt.countDown()
        }.andThen(SINGLE) {
            assertEquals(3, atomicInteger.addAndGet(1))
            assertEquals(threads[SINGLE], Thread.currentThread())
            waitForIt.countDown()
        }.andThen(UI) {
            assertEquals(4, atomicInteger.addAndGet(1))
            assertEquals(threads[UI], Thread.currentThread())
            waitForIt.countDown()
        }
        assertTrue(waitForIt.await(100, TimeUnit.MILLISECONDS))
    }

    /**
     * runs 4 iterations on POOL->SINGLE->UI->POOL
     */
    @Test
    fun continuation_tests_simple_2() {
        val preparation = CountDownLatch(2)
        val threads = mutableMapOf<Executor, Thread>()
        runOn(UI) {
            synchronized(threads) { threads[UI] = Thread.currentThread() }
            preparation.countDown()
        }
        runOn(SINGLE) {
            synchronized(threads) { threads[SINGLE] = Thread.currentThread() }
            preparation.countDown()
        }
        assertTrue(preparation.await(100, TimeUnit.MILLISECONDS))

        // test
        val waitForIt = CountDownLatch(4)
        val atomicInteger = AtomicInteger(0)
        runOn(POOL) {
            assertEquals(1, atomicInteger.addAndGet(1))
            assertNull(threads[POOL])
            waitForIt.countDown()
        }.andThen(SINGLE) {
            assertEquals(2, atomicInteger.addAndGet(1))
            assertEquals(threads[SINGLE], Thread.currentThread())
            waitForIt.countDown()
        }.andThen(UI) {
            assertEquals(3, atomicInteger.addAndGet(1))
            assertEquals(threads[UI], Thread.currentThread())
            waitForIt.countDown()
        }.andThen(POOL) {
            assertEquals(4, atomicInteger.addAndGet(1))
            assertNull(threads[POOL])
            waitForIt.countDown()
        }
        assertTrue(waitForIt.await(100, TimeUnit.MILLISECONDS))
    }

    /**
     * run iteration for every thread
     */
    @Test
    fun continuation_tests_every_threads() {
        val threads = generateThreads()
        val counter = AtomicInteger(0)
        val waitForIt = CountDownLatch(number_of_threads)
        var continuation = runOn(Executor { it.run() }) {}
        for (i in 0 until number_of_threads) {
            val expectedValue = i + 1
            val (executor, expectedThread) = threads[i]
            continuation = continuation.andThen(executor) {
                assertEquals(expectedValue, counter.addAndGet(1))
                assertEquals(expectedThread, Thread.currentThread())
                waitForIt.countDown()
            }
        }
        assertTrue(waitForIt.await(timeout_ms, TimeUnit.MILLISECONDS))
    }

    /**
     * run lots of random iteration
     */
    @Test
    fun continuation_tests_random() {
        val threads = generateThreads()
        val counter = AtomicInteger(0)
        val waitForIt = CountDownLatch(number_of_loops)
        var continuation = runOn(Executor { it.run() }) {}
        for (i in 0 until number_of_loops) {
            val expectedValue = i + 1
            val (executor, expectedThread) = threads[Random.nextInt(threads.size)]
            continuation = continuation.andThen(executor) {
                assertEquals(expectedValue, counter.addAndGet(1))
                assertEquals(expectedThread, Thread.currentThread())
                waitForIt.countDown()
            }
        }
        assertTrue(waitForIt.await(timeout_ms, TimeUnit.MILLISECONDS))
    }

    private fun generateThreads(): List<Pair<Executor, Thread>> {

        val waitForPreparation = CountDownLatch(number_of_threads)
        val threads = mutableListOf<Pair<Executor, Thread>>()
        runOn(UI) {
            synchronized(threads) { threads.add(Pair(UI, Thread.currentThread())) }
            waitForPreparation.countDown()
        }
        runOn(SINGLE) {
            synchronized(threads) { threads.add(Pair(SINGLE, Thread.currentThread())) }
            waitForPreparation.countDown()
        }
        for (i in 2 until number_of_threads) {
            val executioner = Executioner.newSingleExecutioner()
            runOn(executioner) {
                synchronized(threads) { threads.add(Pair(executioner, Thread.currentThread())) }
                waitForPreparation.countDown()
            }
        }
        assertTrue(waitForPreparation.await(100, TimeUnit.MILLISECONDS))
        assertEquals(number_of_threads, threads.size)
        return threads
    }


    companion object {
        private const val number_of_threads = 13
        private const val number_of_loops = 791
        private const val timeout_ms = 600L
    }
}
