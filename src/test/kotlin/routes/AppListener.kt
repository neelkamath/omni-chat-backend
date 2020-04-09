package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.DB
import com.neelkamath.omniChat.tearDown
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.jetbrains.exposed.sql.SchemaUtils

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

private fun DB.tearDown(): Unit = dbTransaction { SchemaUtils.drop(*tables) }