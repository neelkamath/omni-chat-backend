package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.deleteUser
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.subscribeToMessageBroker
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import com.neelkamath.omniChat.db.wipeDb
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
