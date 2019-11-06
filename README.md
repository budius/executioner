# Simple threading on Android

### Add to your project

[ ![Download](https://api.bintray.com/packages/sensorberg/maven/executioner/images/download.svg) ](https://bintray.com/sensorberg/maven/executioner/_latestVersion)

```
implementation 'com.sensorberg.libs:executioner:<latest>'
```

### Usage

```Kotlin
// execute on UI thread
button.setOnClickListener {
    runOn(UI) {
        "Sample 1, executed on UI thread".log()
    }
}

```

```Kotlin
// execute on background thread, and then update UI
button.setOnClickListener {
    runOn(SINGLE) {
        "Sample 2, executing on a single background thread.".log()
        Thread.sleep(600)
        "Sample 2, this just blocked the single background thread for 600ms, don't do that".log()
    }.andThen(UI) {
        "Sample 2, now runs on UI thread, but only after the background execution".log()
    }
}

```

```Kotlin
// execute on a thread pool
button.setOnClickListener {
    repeat(10) {
        runOn(POOL) {
            "Sample 3, running on a thread pool. Part $it".log()
        }
    }
}
```

### Tests
Tests are easy with the executor, just add dependency

```
implementation 'com.sensorberg.libs:executioner-testing:<latest>'

```

and the test rule

```Kotlin
@get:Rule val executionerTestRule = ExecutionerTestRule()
```

And now all the `runOn` and `then` will run sequentially on the test thread.