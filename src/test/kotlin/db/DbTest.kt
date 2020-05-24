package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.deleteUserFromDb
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class DbTest : FunSpec({
    context("deleteUserFromDb(String)") {
        test("An exception should be thrown when the admin of a nonempty group chat deletes their data") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            GroupChats.create(adminId, chat)
            shouldThrowExactly<IllegalArgumentException> { deleteUserFromDb(adminId) }
        }
    }
})