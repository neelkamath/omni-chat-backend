package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.DeletionOfEveryMessage
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.db.count
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.subscriptions.parseFrameData
import com.neelkamath.omniChat.test.graphql.api.subscriptions.receiveMessageUpdates
import com.neelkamath.omniChat.userIdExists
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val DELETE_ACCOUNT_QUERY: String = """
    mutation DeleteAccount {
        deleteAccount
    }
"""

private fun operateDeleteAccount(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(DELETE_ACCOUNT_QUERY, accessToken = accessToken)

fun deleteAccount(accessToken: String): Boolean = operateDeleteAccount(accessToken).data!!["deleteAccount"] as Boolean

class DeleteAccountTest : FunSpec({
    test("An account should be able to be deleted if the user is the admin of an empty group chat") {
        val token = createVerifiedUsers(1)[0].accessToken
        createGroupChat(NewGroupChat("Title"), token)
        deleteAccount(token).shouldBeTrue()
    }

    test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
        val (admin, user) = createVerifiedUsers(2)
        createGroupChat(NewGroupChat("Title", userIdList = setOf(user.info.id)), admin.accessToken)
        deleteAccount(admin.accessToken).shouldBeFalse()
    }

    test("An account should be deleted from the auth service") {
        val user = createVerifiedUsers(1)[0]
        deleteAccount(user.accessToken)
        userIdExists(user.info.id).shouldBeFalse()
    }

    test("The user's contacts, and others' contacts of the user, should be deleted when their account is deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        createContacts(listOf(user2.info.id), user1.accessToken)
        createContacts(listOf(user1.info.id), user2.accessToken)
        deleteAccount(user1.accessToken)
        Contacts.read(user1.info.id).shouldBeEmpty()
        Contacts.read(user2.info.id).shouldBeEmpty()
    }

    test("The user whose account is deleted should be removed from group chats") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        deleteAccount(user.accessToken)
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(admin.info.id)
    }

    test("Group chat messages and message statuses from a deleted account should be deleted") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        val adminMessageId = messageAndReadId(chatId, "text", admin.accessToken)
        messageAndReadId(chatId, "text", user.accessToken)
        createReadStatus(adminMessageId, user.accessToken)
        deleteAccount(user.accessToken)
        val messages = Messages.readChat(chatId)
        messages shouldHaveSize 1
        messages[0].sender.id shouldBe admin.info.id
        messages[0].dateTimes.statuses.shouldBeEmpty()
    }

    test("A private chat with a user who deleted their account should be deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        createPrivateChat(user2.info.id, user1.accessToken)
        deleteAccount(user1.accessToken)
        PrivateChats.readIdList(user2.info.id).shouldBeEmpty()
        PrivateChatDeletions.count().shouldBeZero()
        Messages.count().shouldBeZero()
        MessageStatuses.count().shouldBeZero()
    }

    test("Deleting a user should delete their message update subscriptions for private chats") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        receiveMessageUpdates(chatId, user1.accessToken) { incoming, _ ->
            deleteAccount(user1.accessToken)
            parseFrameData<DeletionOfEveryMessage>(incoming)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Deleting a user should delete their message update subscriptions for group chats") {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        receiveMessageUpdates(chatId, token) { incoming, _ ->
            deleteAccount(token)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})