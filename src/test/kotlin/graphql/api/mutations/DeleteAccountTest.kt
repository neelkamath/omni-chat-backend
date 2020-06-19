package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.DeletionOfEveryMessage
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.api.messageAndReadId
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.api.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.api.subscriptions.receiveMessageUpdates
import com.neelkamath.omniChat.graphql.createSignedInUsers
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

class DeleteAccountTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("An account should be able to be deleted if the user is the admin of an empty group chat") {
        val token = createSignedInUsers(1)[0].accessToken
        createGroupChat(token, NewGroupChat("Title"))
        deleteAccount(token).shouldBeTrue()
    }

    test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
        val (admin, user) = createSignedInUsers(2)
        createGroupChat(admin.accessToken, NewGroupChat("Title", userIdList = listOf(user.info.id)))
        deleteAccount(admin.accessToken).shouldBeFalse()
    }

    test("An account should be deleted from the auth service") {
        val user = createSignedInUsers(1)[0]
        deleteAccount(user.accessToken)
        userIdExists(user.info.id).shouldBeFalse()
    }

    test("The user's contacts, and others' contacts of the user, should be deleted when their account is deleted") {
        val (user1, user2) = createSignedInUsers(2)
        createContacts(user1.accessToken, listOf(user2.info.id))
        createContacts(user2.accessToken, listOf(user1.info.id))
        deleteAccount(user1.accessToken)
        Contacts.read(user1.info.id).shouldBeEmpty()
        Contacts.read(user2.info.id).shouldBeEmpty()
    }

    test("The user whose account is deleted should be removed from group chats") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        deleteAccount(user.accessToken)
        GroupChatUsers.readUserIdList(chatId) shouldBe listOf(admin.info.id)
    }

    test("Group chat messages and message statuses from a deleted account should be deleted") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        val adminMessageId = messageAndReadId(
            admin.accessToken,
            chatId,
            "text"
        )
        messageAndReadId(user.accessToken, chatId, "text")
        createReadStatus(user.accessToken, adminMessageId)
        deleteAccount(user.accessToken)
        val messages = Messages.readGroupChat(chatId)
        messages shouldHaveSize 1
        messages[0].node.sender.id shouldBe admin.info.id
        messages[0].node.dateTimes.statuses.shouldBeEmpty()
    }

    test("A private chat with a user who deleted their account should be deleted") {
        val (user1, user2) = createSignedInUsers(2)
        createPrivateChat(user1.accessToken, user2.info.id)
        deleteAccount(user1.accessToken)
        PrivateChats.readIdList(user2.info.id).shouldBeEmpty()
        PrivateChatDeletions.count().shouldBeZero()
        Messages.count().shouldBeZero()
        MessageStatuses.count().shouldBeZero()
    }

    test("Deleting a user should delete their message update subscriptions for private chats") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        receiveMessageUpdates(user1.accessToken, chatId) { incoming, _ ->
            deleteAccount(user1.accessToken)
            parseFrameData<DeletionOfEveryMessage>(incoming)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Deleting a user should delete their message update subscriptions for group chats") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        receiveMessageUpdates(token, chatId) { incoming, _ ->
            deleteAccount(token)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
}