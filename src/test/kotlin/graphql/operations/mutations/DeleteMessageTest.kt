package graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id)
    }
"""

private fun operateDeleteMessage(accessToken: String, messageId: Int): GraphQlResponse =
    operateGraphQlQueryOrMutation(DELETE_MESSAGE_QUERY, variables = mapOf("id" to messageId), accessToken = accessToken)

fun deleteMessage(accessToken: String, messageId: Int) =
    operateDeleteMessage(accessToken, messageId).data!!["deleteMessage"]

fun errDeleteMessage(accessToken: String, messageId: Int): String =
    operateDeleteMessage(accessToken, messageId).errors!![0].message

class DeleteMessageTest : FunSpec({
    test("The user's message should be deleted") {
        val admin = createSignedInUsers(1)[0]
        val chatId = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val messageId = messageAndReadId(admin.accessToken, chatId, "text")
        deleteMessage(admin.accessToken, messageId)
        Messages.readGroupChat(chatId).shouldBeEmpty()
    }

    test("Deleting a nonexistent message should return an error") {
        val token = createSignedInUsers(1)[0].accessToken
        errDeleteMessage(token, messageId = 0) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a message from a chat the user isn't in should throw an exception") {
        val (user1Token, user2Token) = createSignedInUsers(2).map { it.accessToken }
        val chatId = createGroupChat(user2Token, NewGroupChat("Title"))
        val messageId = messageAndReadId(user2Token, chatId, "text")
        errDeleteMessage(user1Token, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Deleting another user's message should return an error") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(user2.accessToken, chatId, "text")
        errDeleteMessage(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test(
        """
        Given a user who created a private chat, sent a message, and deleted the chat,
        when deleting the message,
        then it should fail
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(user1.accessToken, chatId, "text")
        deletePrivateChat(user1.accessToken, chatId)
        errDeleteMessage(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }
})