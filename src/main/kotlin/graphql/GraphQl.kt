package com.neelkamath.omniChat.graphql

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import java.io.File
import java.time.Instant
import java.util.*

/**
 * Reads the [arg] [T] from the [env].
 *
 * [DataFetchingEnvironment.getArgument] only returns primitives (e.g., [Int], [List]). Use this for data classes.
 */
inline fun <reified T> getArgument(env: DataFetchingEnvironment, arg: String): T =
    jacksonObjectMapper.convertValue(env.getArgument(arg))

/** Throws an [Exception] if the user isn't authorized. */
fun verifyAuth(env: DataFetchingEnvironment) {
    env.getContext<String>() ?: throw UnauthorizedException()
}

fun canDeleteAccount(userId: String): Boolean {
    // If the user is the only one in the chat, then the chat can be deleted without transferring admin status.
    val chatsWithOtherUsers = GroupChats.read(userId).filter { GroupChats.read(it.id).userIdList.size > 1 }
    return userId !in chatsWithOtherUsers.map { it.adminId }
}

fun buildAccountInfo(userId: String): AccountInfo =
    with(Auth.findUserById(userId)) { AccountInfo(id, username, email, firstName, lastName) }

fun buildSchema(): GraphQLSchema = SchemaGenerator().makeExecutableSchema(
    SchemaParser().parse(File("src/main/resources/schema.graphql")),
    newRuntimeWiring()
        .scalar(buildDateTimeScalar())
        .type("Chat", ::wireChat)
        .type("Query", ::wireQuery)
        .type("Mutation", ::wireMutation)
        .build()
)

private fun wireQuery(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("canDeleteAccount", ::canDeleteAccount)
    .dataFetcher("requestJwt", ::requestJwt)
    .dataFetcher("readAccount", ::readAccount)
    .dataFetcher("isUsernameTaken", ::isUsernameTaken)
    .dataFetcher("isEmailTaken", ::isEmailTaken)
    .dataFetcher("readChats", ::readChats)
    .dataFetcher("searchChats", ::searchChats)
    .dataFetcher("readContacts", ::readContacts)
    .dataFetcher("searchContacts", ::searchContacts)
    .dataFetcher("requestJwt", ::requestJwt)
    .dataFetcher("searchUsers", ::searchUsers)

private fun wireMutation(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("deleteAccount", ::deleteAccount)
    .dataFetcher("createAccount", ::createAccount)
    .dataFetcher("updateAccount", ::updateAccount)
    .dataFetcher("verifyEmail", ::verifyEmail)
    .dataFetcher("resetPassword", ::resetPassword)
    .dataFetcher("leaveGroupChat", ::leaveGroupChat)
    .dataFetcher("updateGroupChat", ::updateGroupChat)
    .dataFetcher("createGroupChat", ::createGroupChat)
    .dataFetcher("deletePrivateChat", ::deletePrivateChat)
    .dataFetcher("createPrivateChat", ::createPrivateChat)
    .dataFetcher("message", ::message)
    .dataFetcher("deleteContacts", ::deleteContacts)
    .dataFetcher("createContacts", ::createContacts)

private fun wireChat(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    when (val obj = it.getObject<Any>()) {
        is PrivateChat -> it.schema.getObjectType("PrivateChat")
        is GroupChat -> it.schema.getObjectType("GroupChat")
        else -> throw Error("$obj was neither a PrivateChat, nor a GroupChat.")
    }
}

fun buildDateTimeScalar(): GraphQLScalarType = GraphQLScalarType
    .Builder()
    .name("DateTime")
    .coercing(
        object : Coercing<Date, String> {
            private fun parseDate(iso8601DateTime: String): Date = Date.from(Instant.parse(iso8601DateTime))

            override fun parseValue(input: Any): Date = parseDate(input as String)

            override fun parseLiteral(input: Any): Date = parseDate((input as StringValue).value)

            override fun serialize(dataFetcherResult: Any): String = (dataFetcherResult as Date).toInstant().toString()
        }
    )
    .build()