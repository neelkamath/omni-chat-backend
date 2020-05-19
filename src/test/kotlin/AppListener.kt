package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.test.db.tearDownDb
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class AppListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        setUpDb()
        setUpAuthForTests()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        tearDownDb()
        tearDownAuth()
    }
}