package com.neelkamath.omniChat

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*

/** Project-wide Gson config. */
val gson: Gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // ISO 8601 format.
    .create()

data class Login(val username: String, val password: String)

data class InvalidAccount(val reason: InvalidAccountReason)

enum class InvalidAccountReason {
    NONEXISTENT_USER,
    INCORRECT_PASSWORD,
    EMAIL_NOT_VERIFIED,
    USERNAME_TAKEN,
    EMAIL_TAKEN,
}

data class AuthToken(val jwt: String, val expiry: Date, val refreshToken: String, val refreshTokenExpiry: Date)

data class NewAccount(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class AccountInfo(
    val userId: String,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class AccountUpdate(
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserIdList(val userIdList: Set<String>)

data class UserSearchQuery(
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
)

data class NewGroupChat(val userIdList: Set<String>, val title: String, val description: String? = null)

data class InvalidGroupChat(val reason: InvalidGroupChatReason)

enum class InvalidGroupChatReason {
    EMPTY_USER_ID_LIST,
    INVALID_USER_ID,
    INVALID_TITLE_LENGTH,
    INVALID_DESCRIPTION_LENGTH,
}

data class User(val username: String, val email: String, val firstName: String? = null, val lastName: String? = null)

data class Chats(val chats: List<Chat>)

data class Chat(val type: ChatType, val id: Int)

enum class ChatType { PRIVATE, GROUP }

data class ChatId(val id: Int)

data class GroupChatUpdate(
    val chatId: Int,
    val title: String? = null,
    val description: String? = null,
    val newUserIdList: Set<String>? = null,
    val removedUserIdList: Set<String>? = null,
    val newAdminId: String? = null
)

data class InvalidGroupUpdate(val reason: InvalidGroupUpdateReason)

enum class InvalidGroupUpdateReason { INVALID_CHAT_ID, INVALID_NEW_ADMIN_ID }

data class InvalidPrivateChat(val reason: InvalidPrivateChatReason)

enum class InvalidPrivateChatReason { CHAT_EXISTS, INVALID_USER_ID }

data class InvalidGroupLeave(val reason: InvalidGroupLeaveReason)

enum class InvalidGroupLeaveReason { MISSING_NEW_ADMIN_ID, INVALID_NEW_ADMIN_ID, INVALID_CHAT_ID }

data class GroupChat(
    val adminId: String,
    val userIdList: Set<String>,
    val title: String,
    val description: String? = null
)