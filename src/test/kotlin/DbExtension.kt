package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DbExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext) {
        if (!isSetUp) {
            setUpDb()
            isSetUp = true
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        unsubscribeFromMessageBroker()
        subscribeToMessageBroker()
    }

    override fun afterEach(context: ExtensionContext) {
        val userIdList = Users.read()
        wipeDb()
        userIdList.forEach(::deleteUser)
    }

    private companion object {
        private var isSetUp = false
    }
}