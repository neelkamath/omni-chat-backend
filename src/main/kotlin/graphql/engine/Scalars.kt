package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.Placeholder
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private val scalarDateTime: GraphQLScalarType =
    GraphQLScalarType.Builder().name("DateTime").coercing(DateTimeCoercing).build()

private val scalarCursor: GraphQLScalarType =
    GraphQLScalarType.Builder().name("Cursor").coercing(CursorCoercing).build()

private val scalaraPlaceholder: GraphQLScalarType =
    GraphQLScalarType.Builder().name("Placeholder").coercing(PlaceholderCoercing).build()

private object DateTimeCoercing : Coercing<LocalDateTime, String> {
    private fun parseDate(iso8601DateTime: String): LocalDateTime =
        LocalDateTime.ofInstant(Instant.parse(iso8601DateTime), ZoneOffset.UTC)

    override fun parseValue(input: Any): LocalDateTime = parseDate(input as String)

    override fun parseLiteral(input: Any): LocalDateTime = parseDate((input as StringValue).value)

    override fun serialize(dataFetcherResult: Any): String =
        (dataFetcherResult as LocalDateTime).toInstant(ZoneOffset.UTC).toString()
}

private object CursorCoercing : Coercing<Int, String> {
    override fun parseValue(input: Any): Int = (input as String).toInt()

    override fun parseLiteral(input: Any): Int = (input as StringValue).value.toInt()

    override fun serialize(dataFetcherResult: Any): String = dataFetcherResult.toString()
}

private object PlaceholderCoercing : Coercing<Placeholder, String> {
    override fun parseValue(input: Any) = Placeholder

    override fun parseLiteral(input: Any) = Placeholder

    override fun serialize(dataFetcherResult: Any): String = ""
}

/** Wires GraphQL scalars to the [builder]. */
fun wireGraphQlScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder =
    builder.scalar(scalarDateTime).scalar(scalarCursor).scalar(scalaraPlaceholder)