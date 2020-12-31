package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.deleteUser
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.subscribeToMessageBroker
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import com.neelkamath.omniChat.db.wipeDb
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DbExtension : BeforeAllCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext) {
        setUpDb()
        subscribeToMessageBroker()
    }

    override fun afterEach(context: ExtensionContext) {
        val userIdList = Users.read()
        wipeDb()
        userIdList.forEach(::deleteUser)
    }
}
