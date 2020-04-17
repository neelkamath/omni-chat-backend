package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

class DbListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        Db.setUp()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        Db.tearDown()
    }
}