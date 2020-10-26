package com.neelkamath.omniChat.graphql.engine

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.graphql.routing.GraphQlDocDataException
import com.neelkamath.omniChat.graphql.routing.GraphQlRequest
import com.neelkamath.omniChat.graphql.routing.UnauthorizedException
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.userId
import graphql.*
import graphql.GraphQL.newGraphQL
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring.newRuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import java.lang.ClassLoader.getSystemClassLoader

val graphQl: GraphQL = run {
    val schema = getSystemClassLoader().getResource("schema.graphqls")!!.readText()
    val registry = SchemaParser().parse(schema)
    val wiring = newRuntimeWiring()
    wireGraphQlScalars(wiring)
    wireGraphQlOperations(wiring)
    wireGraphQlTypes(wiring)
    SchemaGenerator().makeExecutableSchema(registry, wiring.build()).let(::newGraphQL).build()
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
    userId ?: throw UnauthorizedException
}

/** The [userId] will be saved as the [ExecutionInput.Builder.context]. */
fun buildExecutionInput(request: GraphQlRequest, userId: Int?): ExecutionInput.Builder = ExecutionInput.Builder()
        .query(request.query)
        .variables(request.variables ?: mapOf())
        .operationName(request.operationName)
        .context(userId)

/**
 * Returns the [ExecutionResult.toSpecification] after masking errors, dealing with `null` `"data"`, and dealing with
 * empty `"errors"`.
 *
 * An [UnauthorizedException] will be thrown if an [UnauthorizedException] is present.
 */
fun buildSpecification(result: ExecutionResult): Map<String, Any> = result.toSpecification()
        .mapValues { if (it.key == "errors") result.errors.map(::maskError) else it.value }
        .filterNot { (it.key == "data" && it.value == null) || (it.key == "errors" && (it.value as List<*>).isEmpty()) }

/**
 * Masks the [error], and returns its [GraphQLError.toSpecification].
 *
 * An [UnauthorizedException] will be thrown if the [error] is an [UnauthorizedException].
 */
private fun maskError(error: GraphQLError): Map<String, Any> {
    val result = error.toSpecification()
    result["message"] = if (error is ExceptionWhileDataFetching) {
        when (error.exception) {
            is UnauthorizedException -> throw UnauthorizedException
            is GraphQlDocDataException -> error.exception.message
            else -> "INTERNAL_SERVER_ERROR"
        }
    } else error.message
    return result
}
