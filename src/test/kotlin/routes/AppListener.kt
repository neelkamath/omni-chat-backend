package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.db.DB
import com.neelkamath.omniChat.test.db.tearDown
import com.neelkamath.omniChat.test.tearDown
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class AppListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        Auth.setUp()
        DB.setUp()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        Auth.tearDown()
        DB.tearDown()
    }
}