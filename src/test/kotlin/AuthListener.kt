package com.neelkamath.omniChat.test

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class AuthListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        setUpAuthForTests()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        tearDownAuth()
    }
}