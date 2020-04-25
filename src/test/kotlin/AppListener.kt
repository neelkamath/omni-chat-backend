package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.test.db.tearDown
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class AppListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        Auth.setUp()
        Db.setUp()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        Auth.tearDown()
        Db.tearDown()
    }
}