package com.neelkamath.omniChat

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule

private object PlaceholderDeserializer : JsonDeserializer<Placeholder>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Placeholder =
        if (parser.text == "") Placeholder
        else throw IllegalArgumentException("""parser.text ("${parser.text}") must be an empty string.""")
}

private object ChatDeserializer : JsonDeserializer<Chat>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Chat {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = if (node.has("users")) GroupChat::class else PrivateChat::class
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object UsernameSerializer : JsonSerializer<Username>() {
    override fun serialize(username: Username, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(username.value)
}

private object UsernameDeserializer : JsonDeserializer<Username>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Username =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Username)
}

private object PasswordSerializer : JsonSerializer<Password>() {
    override fun serialize(password: Password, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(password.value)
}

private object PasswordDeserializer : JsonDeserializer<Password>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Password =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Password)
}

private object GroupChatTitleSerializer : JsonSerializer<GroupChatTitle>() {
    override fun serialize(
        groupChatTitle: GroupChatTitle,
        generator: JsonGenerator,
        provider: SerializerProvider
    ): Unit = generator.writeString(groupChatTitle.value)
}

private object GroupChatTitleDeserializer : JsonDeserializer<GroupChatTitle>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): GroupChatTitle =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::GroupChatTitle)
}

private object GroupChatDescriptionSerializer : JsonSerializer<GroupChatDescription>() {
    override fun serialize(
        groupChatDescription: GroupChatDescription,
        generator: JsonGenerator,
        provider: SerializerProvider
    ): Unit = generator.writeString(groupChatDescription.value)
}

private object GroupChatDescriptionDeserializer : JsonDeserializer<GroupChatDescription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): GroupChatDescription =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::GroupChatDescription)
}

private object TextMessageSerializer : JsonSerializer<TextMessage>() {
    override fun serialize(textMessage: TextMessage, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(textMessage.value)
}

private object TextMessageDeserializer : JsonDeserializer<TextMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): TextMessage =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::TextMessage)
}

private object BioSerializer : JsonSerializer<Bio>() {
    override fun serialize(textMessage: Bio, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(textMessage.value)
}

private object BioDeserializer : JsonDeserializer<Bio>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Bio =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Bio)
}

/** Updates the [objectMapper] to provide the extra functionality the test source set requires. */
fun configureObjectMapper() {
    objectMapper
        .register(Placeholder::class.java, PlaceholderDeserializer)
        .register(Chat::class.java, ChatDeserializer)
        .register(Username::class.java, UsernameDeserializer, UsernameSerializer)
        .register(Password::class.java, PasswordDeserializer, PasswordSerializer)
        .register(GroupChatTitle::class.java, GroupChatTitleDeserializer, GroupChatTitleSerializer)
        .register(GroupChatDescription::class.java, GroupChatDescriptionDeserializer, GroupChatDescriptionSerializer)
        .register(TextMessage::class.java, TextMessageDeserializer, TextMessageSerializer)
        .register(Bio::class.java, BioDeserializer, BioSerializer)
}

/** Convenience function for [ObjectMapper.registerModule]. Registers the [clazz]'s [serializer] and [deserializer]. */
private fun <T> ObjectMapper.register(
    clazz: Class<T>,
    deserializer: JsonDeserializer<T>? = null,
    serializer: JsonSerializer<T>? = null
): ObjectMapper = registerModule(SimpleModule().addSerializer(clazz, serializer).addDeserializer(clazz, deserializer))