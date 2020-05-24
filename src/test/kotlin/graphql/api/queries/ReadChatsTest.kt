package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.test.CreatedUser
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CHATS_QUERY: String = """
    query ReadChats {
        readChats {
            $GROUP_CHAT_FRAGMENT
            $PRIVATE_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChats(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(READ_CHATS_QUERY, accessToken = accessToken)

fun readChats(accessToken: String): List<Chat> {
    val chats = operateReadChats(accessToken).data!!["readChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class ReadChatsTest : FunSpec({
    fun createAdminGroupChat(admin: CreatedUser): GroupChat {
        val user = createVerifiedUsers(1)[0]
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        val users = (chat.userIdList + admin.info.id).map(::findUserById).toSet()
        return GroupChat(chatId, admin.info.id, users, chat.title, chat.description, Messages.readChat(chatId))
    }

    fun createUserGroupChat(admin: CreatedUser): GroupChat {
        val user = createVerifiedUsers(1)[0]
        val chat = NewGroupChat("Title", userIdList = setOf(admin.info.id))
        val chatId = createGroupChat(chat, user.accessToken)
        val users = (chat.userIdList + user.info.id).map(::findUserById).toSet()
        return GroupChat(chatId, user.info.id, users, chat.title, chat.description, Messages.readChat(chatId))
    }

    fun createAdminPrivateChat(admin: CreatedUser): PrivateChat {
        val user = createVerifiedUsers(1)[0]
        val chatId = createPrivateChat(user.info.id, admin.accessToken)
        return PrivateChat(chatId, findUserById(user.info.id), Messages.readChat(chatId))
    }

    fun createUserPrivateChat(admin: CreatedUser): PrivateChat {
        val user = createVerifiedUsers(1)[0]
        val chatId = createPrivateChat(admin.info.id, user.accessToken)
        return PrivateChat(chatId, findUserById(user.info.id), Messages.readChat(chatId))
    }

    /** Creates and returns the [admin]'s chats. */
    fun createChats(admin: CreatedUser): List<Chat> = listOf(
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
})