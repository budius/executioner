package com.sensorberg.executioner.testing

import com.sensorberg.executioner.Executioner
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.Executor

class ExecutionerTestRule : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Executioner.setDelegate(Executor { it.run() })
    }

    override fun finished(description: Description?) {
        Executioner.setDelegate(null)
        super.finished(description)
    }
}
