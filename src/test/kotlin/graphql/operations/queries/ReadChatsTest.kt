package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.PRIVATE_CHAT_FRAGMENT
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
    fun createAdminGroupChat(admin: VerifiedUser): GroupChat {
        val userId = createVerifiedUsers(1)[0].info.id
        val chat = buildNewGroupChat(userId)
        val chatId = GroupChats.create(admin.info.id, chat)
        return GroupChat(
            chatId,
            admin.info.id,
            GroupChatUsers.readUsers(chatId),
            chat.title,
            chat.description,
            Messages.readGroupChatConnection(chatId)
        )
    }

    fun createUserGroupChat(admin: VerifiedUser): GroupChat {
        val user = createVerifiedUsers(1)[0]
        val chat = buildNewGroupChat(admin.info.id)
        val chatId = GroupChats.create(user.info.id, chat)
        return GroupChat(
            chatId,
            user.info.id,
            GroupChatUsers.readUsers(chatId),
            chat.title,
            chat.description,
            Messages.readGroupChatConnection(chatId)
        )
    }

    fun createAdminPrivateChat(admin: VerifiedUser): PrivateChat {
        val userId = createVerifiedUsers(1)[0].info.id
        val chatId = PrivateChats.create(admin.info.id, userId)
        return PrivateChat(
            chatId,
            readUserById(userId), Messages.readPrivateChatConnection(chatId, userId)
        )
    }

    fun createUserPrivateChat(admin: VerifiedUser): PrivateChat {
        val user = createVerifiedUsers(1)[0]
        val chatId = PrivateChats.create(user.info.id, admin.info.id)
        return PrivateChat(
            chatId,
            readUserById(user.info.id), Messages.readPrivateChatConnection(chatId, user.info.id)
        )
    }

    /** Creates and returns the [admin]'s chats. */
    fun createChats(admin: VerifiedUser): List<Chat> = listOf(
        createAdminGroupChat(admin),
        createUserGroupChat(admin),
        createAdminPrivateChat(admin),
        createUserPrivateChat(admin)
    )

    test("Private and group chats the user made, was invited to, and was added to should be retrieved") {
        val admin = createVerifiedUsers(1)[0]
        val chats = createChats(admin)
        readChats(admin.accessToken) shouldBe chats
    }

    test("Private chats deleted by the user should be retrieved only for the other user") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        PrivateChatDeletions.create(chatId, user1.info.id)
        readChats(user1.accessToken).shouldBeEmpty()
        readChats(user2.accessToken).shouldNotBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.READ_CHATS) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
    }
})