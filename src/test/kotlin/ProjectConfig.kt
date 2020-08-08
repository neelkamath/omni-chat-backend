package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.test.TestCase

@AutoScan
@Suppress("unused")
object AppListener : ProjectListener {
    override suspend fun beforeProject() {
        setUpAuth()
        mockEmails()
        setUpDb()
    }

    override suspend fun afterProject() {
        tearDownDb()
        tearDownAuth()
    }
}

@AutoScan
@Suppress("unused")
object DataListener : TestListener {
    override suspend fun afterInvocation(testCase: TestCase, iteration: Int) {
        wipe()
        unsubscribeFromMessageBroker()
        subscribeToMessageBroker()
    }
}

private fun wipe() {
    val userIdList = Users.read()
    wipeDb()
    userIdList.forEach(::deleteUserFromDb)
    wipeAuth()
}