package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.SignedInUser
import com.neelkamath.omniChat.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

const val READ_CHATS_QUERY: String = """
    query ReadChats(
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
    ) {
        readChats {
            $GROUP_CHAT_FRAGMENT
            $PRIVATE_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChats(
    accessToken: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): GraphQlResponse = operateQueryOrMutation(
    READ_CHATS_QUERY,
    variables = mapOf(
        "groupChat_messages_last" to messagesPagination?.last,
        "groupChat_messages_before" to messagesPagination?.before?.toString(),
        "privateChat_messages_last" to messagesPagination?.last,
        "privateChat_messages_before" to messagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString()
    ),
    accessToken = accessToken
)

fun readChats(
    accessToken: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateReadChats(accessToken, usersPagination, messagesPagination).data!!["readChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class ReadChatsTest : FunSpec({
    fun createAdminGroupChat(admin: SignedInUser): GroupChat {
        val userId = createSignedInUsers(1)[0].info.id
        val chat = NewGroupChat("Title", userIdList = listOf(userId))
        val chatId = createGroupChat(admin.accessToken, chat)
        return GroupChat(
            chatId,
            admin.info.id,
            GroupChatUsers.readUsers(chatId),
            chat.title,
            chat.description,
            Messages.readGroupChatConnection(chatId)
        )
    }

    fun createUserGroupChat(admin: SignedInUser): GroupChat {
        val user = createSignedInUsers(1)[0]
        val chat = NewGroupChat("Title", userIdList = listOf(admin.info.id))
        val chatId = createGroupChat(user.accessToken, chat)
        return GroupChat(
            chatId,
            user.info.id,
            GroupChatUsers.readUsers(chatId),
            chat.title,
            chat.description,
            Messages.readGroupChatConnection(chatId)
        )
    }

    fun createAdminPrivateChat(admin: SignedInUser): PrivateChat {
        val userId = createSignedInUsers(1)[0].info.id
        val chatId = createPrivateChat(admin.accessToken, userId)
        return PrivateChat(chatId, findUserById(userId), Messages.readPrivateChatConnection(chatId, userId))
    }

    fun createUserPrivateChat(admin: SignedInUser): PrivateChat {
        val user = createSignedInUsers(1)[0]
        val chatId = createPrivateChat(user.accessToken, admin.info.id)
        return PrivateChat(chatId, findUserById(user.info.id), Messages.readPrivateChatConnection(chatId, user.info.id))
    }

    /** Creates and returns the [admin]'s chats. */
    fun createChats(admin: SignedInUser): List<Chat> = listOf(
        createAdminGroupChat(admin),
        createUserGroupChat(admin),
        createAdminPrivateChat(admin),
        createUserPrivateChat(admin)
    )

    test("Private and group chats the user made, was invited to, and was added to should be retrieved") {
        val admin = createSignedInUsers(1)[0]
        val chats = createChats(admin)
        readChats(admin.accessToken) shouldBe chats
    }

    test("Private chats deleted by the user should be retrieved only for the other user") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        readChats(user1.accessToken).shouldBeEmpty()
        readChats(user2.accessToken).shouldNotBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.READ_CHATS) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
    }
})