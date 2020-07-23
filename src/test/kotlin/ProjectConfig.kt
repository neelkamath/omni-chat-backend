package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.tearDownDb
import com.neelkamath.omniChat.db.wipeDb
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestType

@AutoScan
object AppListener : ProjectListener {
    override suspend fun beforeProject() {
        configureObjectMapper()
        setUpAuthForTests()
        setUpDb()
        wipe() // If previously run tests were cancelled, the data wouldn't have been wiped.
    }

    override suspend fun afterProject() {
        tearDownDb()
        tearDownAuth()
    }
}

@AutoScan
object DataListener : TestListener {
    override suspend fun afterInvocation(testCase: TestCase, iteration: Int) {
        if (testCase.type == TestType.Test) wipe()
    }
}

private fun wipe() {
    wipeDb()
    wipeAuth()
}