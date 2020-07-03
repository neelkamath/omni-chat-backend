package com.neelkamath.omniChat

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.TextMessage

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

/** Updates the [objectMapper] to provide the extra functionality the test source set requires. */
fun configureObjectMapper() {
    objectMapper
        .registerModule(SimpleModule().addDeserializer(Placeholder::class.java, PlaceholderDeserializer))
        .registerModule(SimpleModule().addDeserializer(Chat::class.java, ChatDeserializer))
        .registerModule(
            SimpleModule()
                .addSerializer(Username::class.java, UsernameSerializer)
                .addDeserializer(Username::class.java, UsernameDeserializer)
        )
        .registerModule(
            SimpleModule()
                .addSerializer(Password::class.java, PasswordSerializer)
                .addDeserializer(Password::class.java, PasswordDeserializer)
        )
        .registerModule(
            SimpleModule()
                .addSerializer(GroupChatTitle::class.java, GroupChatTitleSerializer)
                .addDeserializer(GroupChatTitle::class.java, GroupChatTitleDeserializer)
        )
        .registerModule(
            SimpleModule()
                .addSerializer(GroupChatDescription::class.java, GroupChatDescriptionSerializer)
                .addDeserializer(GroupChatDescription::class.java, GroupChatDescriptionDeserializer)
        )
        .registerModule(
            SimpleModule()
                .addSerializer(TextMessage::class.java, TextMessageSerializer)
                .addDeserializer(TextMessage::class.java, TextMessageDeserializer)
        )
}