package com.sensorberg.executioner

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.*

object Executioner {

    //region - executor delegation
    private var delegate: Executor? = null

    fun setDelegate(executor: Executor?) {
        delegate = executor
    }
    //endregion

    //region - main API (runOn, wrap)
    fun runOn(executor: Executor, command: Runnable): Continuation {
        val continuation = Continuation()
        continuation.enqueue(executor, command)
        return continuation
    }

    fun runOn(executor: Executor, command: () -> Unit): Continuation {
        return runOn(executor, Runnable { command() })
    }

    fun wrap(executor: Executor): Executor {
        return WrapExecutor(executor)
    }
    //endregion

    //region - available executors
    val UI: Executor by lazy {
        wrap(if (isAndroid())
            AndroidUi()
        else
            Executor { it.run() })
    }

    val SINGLE: Executor by lazy { newSingleExecutioner() }
    val POOL: Executor by lazy { newExecutionerPool() }

    fun newSingleExecutioner(): Executor {
        return wrap(
            if (isAndroid()) {
                AndroidSingleThreadExecutor()
            } else {
                Executors.newSingleThreadExecutor()
            }
        )
    }

    fun newExecutionerPool(): Executor {
        var cores = Runtime.getRuntime().availableProcessors()
        // I heard rumours of availableProcessors() returning garbage,
        // so let's make sure it's some sensible numbers
        if (cores <= 0) cores = 1
        if (cores > 16) cores = 16
        val keepAlive = 3L
        return wrap(
            ThreadPoolExecutor(
                cores,
                cores,
                keepAlive,
                TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>()
            )
        )
    }
    //endregion

    //region - continuation class
    class Continuation {

        @Volatile
        private var awaitingOperation = false
        private val actions = mutableListOf<Pair<Executor, Runnable>>()
        private var command: Runnable? = null
        private val execution = Runnable {
            command!!.run()
            command = null
            // dequeue
            synchronized(this) {
                awaitingOperation = false
                if (actions.isNotEmpty()) {
                    val (executor, command) = actions.removeAt(0)
                    execute(executor, command)
                }
            }
        }

        private fun execute(executor: Executor, command: Runnable) {
            if (awaitingOperation) throw IllegalStateException("Already awaiting for another operation")
            awaitingOperation = true
            this.command = command
            executor.execute(execution)
        }

        internal fun enqueue(executor: Executor, command: Runnable) {
            if (executor !is WrapExecutor) {
                Timber.w(notWrappedExecutorWarningMessage)
            }
            synchronized(this) {
                if (awaitingOperation) {
                    actions.add(Pair(executor, command))
                } else {
                    execute(executor, command)
                }
            }
        }

        fun andThen(executor: Executor, command: Runnable): Continuation {
            enqueue(executor, command)
            return this
        }

        fun andThen(executor: Executor, command: () -> Unit): Continuation {
            return andThen(executor, Runnable { command() })
        }
    }
    //endregion

    //region - helper classes
    private class WrapExecutor(private val executor: Executor) : Executor {
        override fun execute(command: Runnable) {
            val delegate = delegate
            if (delegate == null) {
                executor.execute(command)
            } else {
                delegate.execute(command)
            }
        }
    }

    internal class AndroidUi : Executor {
        private val handler by lazy { Handler(Looper.getMainLooper()) }
        override fun execute(command: Runnable) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                command.run()
            } else {
                handler.post(command)
            }
        }
    }

    internal class AndroidSingleThreadExecutor : Executor {
        private val thread = HandlerThread("single-executor").apply { start() }
        private val handler by lazy { Handler(thread.looper) }
        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }
    //endregion

    //region - helper methods
    private fun isAndroid(): Boolean {
        return (System.getProperty("java.vm.vendor")?.contains("Android") == true)
    }

    internal const val notWrappedExecutorWarningMessage =
        "Executing command on a normal java Executor. To support better testability it's highly advisable to run on an executor acquired through the Executioner class"
    //endregion
}
