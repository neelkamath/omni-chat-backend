package graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.chats.GroupChatUsers
import com.neelkamath.omniChat.db.chats.PrivateChatDeletions
import com.neelkamath.omniChat.db.chats.PrivateChats
import com.neelkamath.omniChat.db.chats.count
import com.neelkamath.omniChat.db.contacts.Contacts
import com.neelkamath.omniChat.db.messages.MessageStatuses
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.db.messages.count
import com.neelkamath.omniChat.graphql.CannotDeleteAccountException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.userIdExists
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

fun deleteAccount(accessToken: String) = operateDeleteAccount(accessToken).data!!["deleteAccount"]

fun errDeleteAccount(accessToken: String): String = operateDeleteAccount(accessToken).errors!![0].message

class DeleteAccountTest : FunSpec({
    test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
        val (admin, user) = createSignedInUsers(2)
        createGroupChat(admin.accessToken, NewGroupChat("Title", userIdList = listOf(user.info.id)))
        errDeleteAccount(admin.accessToken) shouldBe CannotDeleteAccountException.message
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
        Contacts.readIdList(user1.info.id).shouldBeEmpty()
        Contacts.readIdList(user2.info.id).shouldBeEmpty()
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
        val adminMessageId = messageAndReadId(admin.accessToken, chatId, "text")
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
})