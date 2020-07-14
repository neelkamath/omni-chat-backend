package com.neelkamath.omniChat.graphql.engine

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.routing.readGraphQlHttpResponse
import graphql.ExecutionInput

/**
 * Executes GraphQL queries and mutations directly via the GraphQL engine.
 *
 * @param[query] GraphQL document.
 * @param[variables] GraphQL variables for the [query].
 * @param[userId] the ID of the user performing the operation.
 * @see [readGraphQlHttpResponse]
 */
fun executeGraphQlViaEngine(
    query: String,
    variables: Map<String, Any?>? = null,
    userId: Int? = null
): GraphQlResponse {
    val builder = ExecutionInput.Builder().query(query).context(userId)
    variables?.let { objectMapper.convertValue<Map<String, Any>>(it).let(builder::variables) }
    val spec = buildSpecification(graphQl.execute(builder))
    return objectMapper.convertValue(spec)
}