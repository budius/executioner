package com.sensorberg.executioner.sample

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.sensorberg.executioner.Executioner.POOL
import com.sensorberg.executioner.Executioner.SINGLE
import com.sensorberg.executioner.Executioner.UI
import com.sensorberg.executioner.Executioner.runOn

/**
 * Very pointless sample on how to use executioner.
 * Don't run it on Activity. Use a ViewModel.
 */
class SampleActivity : AppCompatActivity() {

    private fun String.log() {
        Log.d("Executioner", this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        findViewById<Button>(R.id.btn1)?.let { setupSample1(it) }
        findViewById<Button>(R.id.btn2)?.let { setupSample2(it) }
        findViewById<Button>(R.id.btn3)?.let { setupSample3(it) }
        findViewById<Button>(R.id.btn4)?.let { setupSample4(it) }
    }

    private fun setupSample1(button: Button) {
        button.text = "Sample 1 - Run on UI thread"
        button.setOnClickListener {
            runOn(UI) {
                "Sample 1, executed on UI thread".log()
            }
        }
    }

    private fun setupSample2(button: Button) {
        button.text = "Sample 2 - Run on Single background thread, then UI"
        button.setOnClickListener {
            runOn(SINGLE) {
                "Sample 2, executing on a single background thread.".log()
                Thread.sleep(600)
                "Sample 2, this just blocked the single background thread for 600ms, don't that".log()
            }.andThen(UI) {
                "Sample 2, now runs on UI thread, but only after the background execution".log()
            }
        }
    }

    private fun setupSample3(button: Button) {
        button.text = "Sample 3 - 10x (Run on thread pool, then single background)"
        button.setOnClickListener {
            repeat(10) {
                val executioner = runOn(POOL) {
                    "Sample 3, running on a thread pool. Part $it".log()
                }
                if (it == 5) {
                    executioner.andThen(SINGLE) {
                        "Sample 3, after 5th execution, this runs on the single background thread".log()
                    }
                }
            }
        }
    }

    private fun setupSample4(button: Button) {
        button.text = "Sample 3 - Let's go crazy!"
        button.setOnClickListener {
            runOn(SINGLE) {
                "Sample 4, running on single background".log()
            }.andThen(POOL) {
                "Sample 4, and then running on thread pool".log()
            }.andThen(SINGLE) {
                "Sample 4, and then running on single background".log()
            }.andThen(POOL) {
                "Sample 4, and then running on thread pool".log()
            }.andThen(UI) {
                "Sample 4, and then running on UI thread".log()
            }.andThen(SINGLE) {
                "Sample 4, and then running on single background".log()
            }
        }
    }
}
