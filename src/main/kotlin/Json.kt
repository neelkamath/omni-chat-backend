package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*

/** Project-wide Jackson config. */
val jacksonObjectMapper: ObjectMapper = jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

data class GraphQlRequest(
    /** GraphQL document (e.g., a mutation). */
    val query: String,
    val variables: Map<String, Any>? = null,
    val operationName: String? = null
)

data class Login(val username: String, val password: String)

data class AuthToken(val jwt: String, val expiry: Date, val refreshToken: String, val refreshTokenExpiry: Date)

data class NewAccount(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class AccountInfo(
    val id: String,
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

data class NewGroupChat(val title: String, val description: String? = null, val userIdList: Set<String> = setOf())

data class GroupChatUpdate(
    val chatId: Int,
    val title: String? = null,
    val description: String? = null,
    val newUserIdList: Set<String> = setOf(),
    val removedUserIdList: Set<String> = setOf(),
    val newAdminId: String? = null
)

interface Chat

data class PrivateChat(
    val id: Int,
    /** The ID of the user being chatted with. */
    val userId: String
) : Chat

data class GroupChat(
    val id: Int,
    val adminId: String,
    val userIdList: Set<String>,
    val title: String,
    val description: String? = null
) : Chat