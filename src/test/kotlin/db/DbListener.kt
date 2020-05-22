package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.setUpDb
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.core.test.TestType

class DbListener : TestListener {
    override suspend fun beforeTest(testCase: TestCase) {
        if (testCase.type == TestType.Test) setUpDb()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        if (testCase.type == TestType.Test) tearDownDb()
    }
}