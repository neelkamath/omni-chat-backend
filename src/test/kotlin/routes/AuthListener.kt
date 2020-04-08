package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.tearDown
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class AuthListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        Auth.setUp()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        Auth.tearDown()
    }
}