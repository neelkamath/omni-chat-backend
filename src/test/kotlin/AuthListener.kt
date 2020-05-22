package com.neelkamath.omniChat.test

import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType

class AuthListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.type == TestType.Test) setUpAuthForTests()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Test) tearDownAuth()
    }
}