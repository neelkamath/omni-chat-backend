package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.setUpDb
import com.neelkamath.omniChatBackend.db.subscribeToMessageBroker
import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.db.tables.read
import com.neelkamath.omniChatBackend.db.wipeDb
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DbExtension : BeforeAllCallback, BeforeEachCallback {
    override fun beforeAll(context: ExtensionContext) {
        setUpDb()
        subscribeToMessageBroker()
    }

    override fun beforeEach(context: ExtensionContext) {
        val userIdList = Users.read()
        /*
        If the test runner was aborted mid-run, then it will not run an <afterEach()> callback since the test case it
        was executing never completed. It's also possible the DB was populated manually prior to the automated test
        suite got run. It's for these reasons we wipe the DB in the <beforeEach()> callback instead.
         */
        wipeDb()
        userIdList.forEach(::deleteUser)
    }
}
