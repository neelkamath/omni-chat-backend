package com.neelkamath.omniChat.graphql

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import graphql.*
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import java.lang.ClassLoader.getSystemClassLoader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private val scalarDateTime: GraphQLScalarType =
    GraphQLScalarType.Builder().name("DateTime").coercing(DateTimeCoercing()).build()

val graphQl: GraphQL = run {
    val schemaInput = getSystemClassLoader().getResource("schema.graphqls")!!.readText()
    val registry = SchemaParser().parse(schemaInput)
    val wiring = newRuntimeWiring()
        .scalar(scalarDateTime)
        .type("Chat", ::wireTypeChat)
        .type("MessageUpdatesInfo", ::wireTypeMessageUpdatesInfo)
        .type("Query", ::wireQuery)
        .type("Mutation", ::wireMutation)
        .type("Subscription", ::wireSubscription)
        .build()
    val schema = SchemaGenerator().makeExecutableSchema(registry, wiring)
    GraphQL.newGraphQL(schema).build()
}

private class DateTimeCoercing : Coercing<LocalDateTime, String> {
    private fun parseDate(iso8601DateTime: String): LocalDateTime =
        LocalDateTime.ofInstant(Instant.parse(iso8601DateTime), ZoneOffset.UTC)

    override fun parseValue(input: Any): LocalDateTime = parseDate(input as String)

    override fun parseLiteral(input: Any): LocalDateTime = parseDate((input as StringValue).value)

    override fun serialize(dataFetcherResult: Any): String =
        (dataFetcherResult as LocalDateTime).toInstant(ZoneOffset.UTC).toString()
}

/**
 * [DataFetchingEnvironment.getArgument] only returns primitives (e.g., [Int], [List]). Use this for [Set]s, data
 * classes, etc.
 */
inline fun <reified T> DataFetchingEnvironment.parseArgument(arg: String): T =
    objectMapper.convertValue(getArgument(arg))

/**
 * Throws an [UnauthorizedException] if the user isn't authenticated.
 *
 * You should call this at the beginning of a [DataFetchingEnvironment] which requires authentication so that you know
 * that [DataFetchingEnvironment.userId] isn't `null`, and the user will receive a friendly error message if they didn't
 * pass valid credentials.
 */
fun DataFetchingEnvironment.verifyAuth() {
    userId ?: throw UnauthorizedException()
}

private fun wireQuery(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("canDeleteAccount", ::canDeleteAccount)
    .dataFetcher("readAccount", ::readAccount)
    .dataFetcher("isUsernameTaken", ::isUsernameTaken)
    .dataFetcher("isEmailAddressTaken", ::isEmailAddressTaken)
    .dataFetcher("readChats", ::readChats)
    .dataFetcher("searchChats", ::searchChats)
    .dataFetcher("readContacts", ::readContacts)
    .dataFetcher("searchContacts", ::searchContacts)
    .dataFetcher("searchMessages", ::searchMessages)
    .dataFetcher("requestTokenSet", ::requestTokenSet)
    .dataFetcher("refreshTokenSet", ::refreshTokenSet)
    .dataFetcher("searchChatMessages", ::searchChatMessages)
    .dataFetcher("searchUsers", ::searchUsers)

private fun wireMutation(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("deleteAccount", ::deleteAccount)
    .dataFetcher("createAccount", ::createAccount)
    .dataFetcher("updateAccount", ::updateAccount)
    .dataFetcher("sendEmailAddressVerification", ::sendEmailAddressVerification)
    .dataFetcher("resetPassword", ::resetPassword)
    .dataFetcher("leaveGroupChat", ::leaveGroupChat)
    .dataFetcher("updateGroupChat", ::updateGroupChat)
    .dataFetcher("createDeliveredStatus", ::createDeliveredStatus)
    .dataFetcher("createReadStatus", ::createReadStatus)
    .dataFetcher("createGroupChat", ::createGroupChat)
    .dataFetcher("deletePrivateChat", ::deletePrivateChat)
    .dataFetcher("createPrivateChat", ::createPrivateChat)
    .dataFetcher("createMessage", ::createMessage)
    .dataFetcher("deleteContacts", ::deleteContacts)
    .dataFetcher("createContacts", ::createContacts)
    .dataFetcher("deleteMessage", ::deleteMessage)

private fun wireSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.dataFetcher("messageUpdates", ::operateMessageUpdates)

private fun wireTypeChat(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    when (val obj = it.getObject<Any>()) {
        is PrivateChat -> it.schema.getObjectType("PrivateChat")
        is GroupChat -> it.schema.getObjectType("GroupChat")
        else -> throw Error("$obj was neither a PrivateChat, nor a GroupChat.")
    }
}

private fun wireTypeMessageUpdatesInfo(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> it.schema.getObjectType("CreatedSubscription")
            is Message -> it.schema.getObjectType("Message")
            is DeletedMessage -> it.schema.getObjectType("DeletedMessage")
            is MessageDeletionPoint -> it.schema.getObjectType("MessageDeletionPoint")
            is UserChatMessagesRemoval -> it.schema.getObjectType("UserChatMessagesRemoval")
            is DeletionOfEveryMessage -> it.schema.getObjectType("DeletionOfEveryMessage")
            else -> throw Error(
                """
                $obj wasn't a CreatedSubscription, Message, DeletedMessage, MessageDeletionPoint, 
                UserChatMessagesRemoval, or DeletionOfEveryMessage.
                """.trimIndent()
            )
        }
    }

/** If there's a [JWTPrincipal] in the [call], the JWT's `sub` will be saved as the [ExecutionInput.Builder.context]. */
fun buildExecutionInput(request: GraphQlRequest, call: ApplicationCall): ExecutionInput.Builder =
    ExecutionInput.Builder()
        .query(request.query)
        .variables(request.variables ?: mapOf())
        .operationName(request.operationName)
        .context(call.authentication.principal<JWTPrincipal>()?.payload?.subject)

/** Returns the [ExecutionResult.toSpecification] after masking errors, and dealing with `null` `"data"`/`"errors"`. */
fun buildSpecification(result: ExecutionResult): Map<String, Any> = result.toSpecification()
    .mapValues { if (it.key == "errors") result.errors.map(::maskError) else it.value }
    .filterNot {
        (it.key == "data" && it.value == null) || (it.key == "errors" && (it.value as List<*>).isEmpty())
    }

/** Masks the [error], and returns its [GraphQLError.toSpecification]. */
private fun maskError(error: GraphQLError): Map<String, Any> {
    val result = error.toSpecification()
    result["message"] = when {
        error is ExceptionWhileDataFetching && error.exception is ClientException -> error.exception.message
        error is ExceptionWhileDataFetching -> "INTERNAL_SERVER_ERROR"
        else -> error.message
    }
    return result
}