package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.*
import graphql.language.StringValue
import graphql.schema.*
import graphql.schema.idl.RuntimeWiring
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private object DateTimeCoercing : Coercing<LocalDateTime, String> {
    private fun parseDate(iso8601DateTime: String): LocalDateTime =
        LocalDateTime.ofInstant(Instant.parse(iso8601DateTime), ZoneOffset.UTC)

    override fun parseValue(input: Any): LocalDateTime = dissectValue { parseDate(input as String) }

    override fun parseLiteral(input: Any): LocalDateTime = dissectLiteral { parseDate((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String =
        translate { (dataFetcherResult as LocalDateTime).toInstant(ZoneOffset.UTC).toString() }
}

private object CursorCoercing : Coercing<Cursor, String> {
    override fun parseValue(input: Any): Cursor = dissectValue { (input as String).toInt() }

    override fun parseLiteral(input: Any): Cursor = dissectLiteral { (input as StringValue).value.toInt() }

    override fun serialize(dataFetcherResult: Any): String = translate { dataFetcherResult.toString() }
}

private object PlaceholderCoercing : Coercing<Placeholder, String> {
    /** An [IllegalArgumentException] will be thrown if the [input] isn't an empty [String]. */
    private fun parse(input: String): Placeholder {
        if (input != "") throw IllegalArgumentException("""input ("$input") must be an empty string.""")
        return Placeholder
    }

    override fun parseValue(input: Any): Placeholder = dissectValue { parse(input as String) }

    override fun parseLiteral(input: Any): Placeholder = dissectLiteral { parse((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate {
        if (dataFetcherResult !is Placeholder)
            throw IllegalArgumentException("The dataFetcherResult ($dataFetcherResult) must be a Placeholder.")
        ""
    }
}

private object UsernameCoercing : Coercing<Username, String> {
    override fun parseValue(input: Any): Username = dissectValue { Username(input as String) }

    override fun parseLiteral(input: Any): Username = dissectLiteral { Username((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate { (dataFetcherResult as Username).value }
}

private object PasswordCoercing : Coercing<Password, String> {
    override fun parseValue(input: Any): Password = dissectValue { Password(input as String) }

    override fun parseLiteral(input: Any): Password = dissectLiteral { Password((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate { (dataFetcherResult as Password).value }
}

private object GroupChatTitleCoercing : Coercing<GroupChatTitle, String> {
    override fun parseValue(input: Any): GroupChatTitle = dissectValue { GroupChatTitle(input as String) }

    override fun parseLiteral(input: Any): GroupChatTitle =
        dissectLiteral { GroupChatTitle((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate { (dataFetcherResult as GroupChatTitle).value }
}

private object GroupChatDescriptionCoercing : Coercing<GroupChatDescription, String> {
    override fun parseValue(input: Any): GroupChatDescription = dissectValue { GroupChatDescription(input as String) }

    override fun parseLiteral(input: Any): GroupChatDescription =
        dissectLiteral { GroupChatDescription((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String =
        translate { (dataFetcherResult as GroupChatDescription).value }
}

private object TextMessageCoercing : Coercing<TextMessage, String> {
    override fun parseValue(input: Any): TextMessage = dissectValue { TextMessage(input as String) }

    override fun parseLiteral(input: Any): TextMessage = dissectLiteral { TextMessage((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate { (dataFetcherResult as TextMessage).value }
}

private object BioCoercing : Coercing<Bio, String> {
    override fun parseValue(input: Any): Bio = dissectValue { Bio(input as String) }

    override fun parseLiteral(input: Any): Bio = dissectLiteral { Bio((input as StringValue).value) }

    override fun serialize(dataFetcherResult: Any): String = translate { (dataFetcherResult as Bio).value }
}

/**
 * The [parse]d value will be returned. A [CoercingParseValueException] will be thrown if [parse] threw an [Exception].
 */
private inline fun <T> dissectValue(parse: () -> T): T = try {
    parse()
} catch (exception: Exception) {
    throw CoercingParseValueException(exception.message)
}

/** The [parse]d data will be returned. An [IllegalArgumentException] will be thrown if [parse] threw an [Exception]. */
private inline fun <T> dissectLiteral(parse: () -> T): T = try {
    parse()
} catch (exception: Exception) {
    throw CoercingParseLiteralException(exception.message)
}

/**
 * Returns the [serialize]d value. A [CoercingSerializeException] will be thrown if [serialize] threw an [Exception].
 */
private inline fun <T> translate(serialize: () -> T): T = try {
    serialize()
} catch (exception: Exception) {
    throw CoercingSerializeException(exception.message)
}

/** Wires GraphQL scalars to the [builder]. */
fun wireGraphQlScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .scalar(build("DateTime", DateTimeCoercing))
    .scalar(build("Cursor", CursorCoercing))
    .scalar(build("Placeholder", PlaceholderCoercing))
    .scalar(build("Username", UsernameCoercing))
    .scalar(build("Password", PasswordCoercing))
    .scalar(build("GroupChatTitle", GroupChatTitleCoercing))
    .scalar(build("GroupChatDescription", GroupChatDescriptionCoercing))
    .scalar(build("TextMessage", TextMessageCoercing))
    .scalar(build("Bio", BioCoercing))

private fun build(name: String, coercing: Coercing<*, *>): GraphQLScalarType =
    GraphQLScalarType.Builder().name(name).coercing(coercing).build()