package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.tearDownDb
import com.neelkamath.omniChat.db.wipeDb
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType

object ProjectConfig : AbstractProjectConfig() {
    override fun listeners(): List<Listener> = listOf(AppListener)

    override fun beforeAll() {
        configureObjectMapper()
        setUpAuthForTests()
        setUpDb()
        wipe() // If previously run tests were cancelled, the data wouldn't have been wiped.
    }

    override fun afterAll() {
        tearDownDb()
        tearDownAuth()
    }
}

private object AppListener : TestListener {
    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Test) wipe()
    }
}

/** Calls [wipeAuth] and [wipeDb]. */
private fun wipe() {
    wipeDb()
    wipeAuth()
}