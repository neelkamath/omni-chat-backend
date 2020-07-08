package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.CannotDeleteAccountException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

const val DELETE_ACCOUNT_QUERY = """
    mutation DeleteAccount {
        deleteAccount
    }
"""

private fun operateDeleteAccount(accessToken: String): GraphQlResponse =
    operateGraphQlQueryOrMutation(DELETE_ACCOUNT_QUERY, accessToken = accessToken)

fun deleteAccount(accessToken: String): Placeholder {
    val data = operateDeleteAccount(accessToken).data!!["deleteAccount"] as String
    return objectMapper.convertValue(data)
}

fun errDeleteAccount(accessToken: String): String = operateDeleteAccount(accessToken).errors!![0].message

class DeleteAccountTest : FunSpec({
    test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
        val (admin, user) = createVerifiedUsers(2)
        GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        errDeleteAccount(admin.accessToken) shouldBe CannotDeleteAccountException.message
    }

    test("An account should be deleted from the auth service") {
        val user = createVerifiedUsers(1)[0]
        deleteAccount(user.accessToken)
        userIdExists(user.info.id).shouldBeFalse()
    }

    test("The user's contacts, and others' contacts of the user, should be deleted when their account is deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        Contacts.create(user1.info.id, setOf(user2.info.id))
        Contacts.create(user1.info.id, setOf(user1.info.id))
        deleteAccount(user1.accessToken)
        Contacts.readIdList(user1.info.id).shouldBeEmpty()
        Contacts.readIdList(user2.info.id).shouldBeEmpty()
    }

    test("The user whose account is deleted should be removed from group chats") {
        val (admin, user) = createVerifiedUsers(2)
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        deleteAccount(user.accessToken)
        GroupChatUsers.readUserIdList(chatId) shouldBe listOf(admin.info.id)
    }

    test("Group chat messages and message statuses from a deleted account should be deleted") {
        val (admin, user) = createVerifiedUsers(2)
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        val adminMessageId = Messages.message(chatId, admin.info.id, TextMessage("t"))
        Messages.message(chatId, user.info.id, TextMessage("t"))
        MessageStatuses.create(adminMessageId, user.info.id, MessageStatus.READ)
        deleteAccount(user.accessToken)
        val messages = Messages.readGroupChat(chatId)
        messages shouldHaveSize 1
        messages[0].node.sender.id shouldBe admin.info.id
        messages[0].node.dateTimes.statuses.shouldBeEmpty()
    }

    test("A private chat with a user who deleted their account should be deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        PrivateChats.create(user1.info.id, user2.info.id)
        deleteAccount(user1.accessToken)
        PrivateChats.readIdList(user2.info.id).shouldBeEmpty()
        PrivateChatDeletions.count().shouldBeZero()
        Messages.count().shouldBeZero()
        MessageStatuses.count().shouldBeZero()
    }
})