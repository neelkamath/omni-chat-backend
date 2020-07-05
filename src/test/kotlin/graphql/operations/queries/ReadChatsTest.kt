package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.graphql.SignedInUser
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

const val READ_CHATS_QUERY = """
    query ReadChats(
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChats {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChats(
    accessToken: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    READ_CHATS_QUERY,
    variables = mapOf(
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    accessToken = accessToken
)

fun readChats(
    accessToken: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateReadChats(
        accessToken,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["readChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class ReadChatsTest : FunSpec({
    fun createAdminGroupChat(admin: SignedInUser): GroupChat {
        val userId = createSignedInUsers(1)[0].info.id
        val chat = buildNewGroupChat(userId)
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
        val chat = buildNewGroupChat(admin.info.id)
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
        return PrivateChat(
            chatId,
            readUserById(userId), Messages.readPrivateChatConnection(chatId, userId)
        )
    }

    fun createUserPrivateChat(admin: SignedInUser): PrivateChat {
        val user = createSignedInUsers(1)[0]
        val chatId = createPrivateChat(user.accessToken, admin.info.id)
        return PrivateChat(
            chatId,
            readUserById(user.info.id), Messages.readPrivateChatConnection(chatId, user.info.id)
        )
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