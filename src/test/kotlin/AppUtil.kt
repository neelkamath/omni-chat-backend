package com.neelkamath.omniChat

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChat.db.unsubscribeFromMessageBroker
import com.neelkamath.omniChat.graphql.routing.*
import io.ktor.application.*
import java.util.*
import kotlin.reflect.KClass

/** The [objectMapper] for the test source set. */
val testingObjectMapper: ObjectMapper = objectMapper
    .copy()
    .register(PlaceholderDeserializer)
    .register(ChatDeserializer)
    .register(AccountDataDeserializer)
    .register(BareMessageDeserializer)
    .register(MessageDeserializer)
    .register(BareChatMessageDeserializer)
    .register(StarredMessageDeserializer)
    .register(NewMessageDeserializer)
    .register(UpdatedMessageDeserializer)
    .register(UsernameDeserializer, UsernameSerializer)
    .register(NameDeserializer, NameSerializer)
    .register(PasswordDeserializer, PasswordSerializer)
    .register(GroupChatTitleDeserializer, GroupChatTitleSerializer)
    .register(GroupChatDescriptionDeserializer, GroupChatDescriptionSerializer)
    .register(MessageTextDeserializer, MessageTextSerializer)
    .register(BioDeserializer, BioSerializer)
    .register(UuidDeserializer, UuidSerializer)

/** Use in place of [Application.main]. */
fun Application.test() {
    unsubscribeFromMessageBroker()
    main()
}

private object PlaceholderDeserializer : JsonDeserializer<Placeholder>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Placeholder =
        if (parser.text == "") Placeholder
        else throw IllegalArgumentException("""parser.text ("${parser.text}") must be an empty string.""")
}

private object ChatDeserializer : JsonDeserializer<Chat>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Chat {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "PrivateChat" -> PrivateChat::class
            "GroupChat" -> GroupChat::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object AccountDataDeserializer : JsonDeserializer<AccountData>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): AccountData {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "Account" -> Account::class
            "NewContact" -> NewContact::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object BareMessageDeserializer : JsonDeserializer<BareMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BareMessage {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "TextMessage" -> TextMessage::class
            "PicMessage" -> PicMessage::class
            "PollMessage" -> PollMessage::class
            "AudioMessage" -> AudioMessage::class
            "StarredTextMessage" -> StarredTextMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            "NewTextMessage" -> NewTextMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "UpdatedTextMessage" -> UpdatedTextMessage::class
            "UpdatedPicMessage" -> UpdatedPicMessage::class
            "UpdatedPollMessage" -> UpdatedPollMessage::class
            "UpdatedAudioMessage" -> UpdatedAudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object MessageDeserializer : JsonDeserializer<Message>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Message {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "TextMessage" -> TextMessage::class
            "PicMessage" -> PicMessage::class
            "PollMessage" -> PollMessage::class
            "AudioMessage" -> AudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object BareChatMessageDeserializer : JsonDeserializer<BareChatMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BareChatMessage {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "StarredTextMessage" -> StarredTextMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            "NewTextMessage" -> NewTextMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "UpdatedTextMessage" -> UpdatedTextMessage::class
            "UpdatedPicMessage" -> UpdatedPicMessage::class
            "UpdatedPollMessage" -> UpdatedPollMessage::class
            "UpdatedAudioMessage" -> UpdatedAudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object StarredMessageDeserializer : JsonDeserializer<StarredMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): StarredMessage {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "StarredTextMessage" -> StarredTextMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object NewMessageDeserializer : JsonDeserializer<NewMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): NewMessage {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out NewMessage> = when (val type = node["__typename"].asText()) {
            "NewTextMessage" -> NewTextMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object UpdatedMessageDeserializer : JsonDeserializer<UpdatedMessage>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): UpdatedMessage {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out UpdatedMessage> = when (val type = node["__typename"].asText()) {
            "UpdatedTextMessage" -> UpdatedTextMessage::class
            "UpdatedPicMessage" -> UpdatedPicMessage::class
            "UpdatedPollMessage" -> UpdatedPollMessage::class
            "UpdatedAudioMessage" -> UpdatedAudioMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object UsernameDeserializer : JsonDeserializer<Username>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Username =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Username)
}

private object UsernameSerializer : JsonSerializer<Username>() {
    override fun serialize(username: Username, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(username.value)
}

private object NameDeserializer : JsonDeserializer<Name>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Name =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Name)
}

private object NameSerializer : JsonSerializer<Name>() {
    override fun serialize(name: Name, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(name.value)
}

private object PasswordDeserializer : JsonDeserializer<Password>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Password =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Password)
}

private object PasswordSerializer : JsonSerializer<Password>() {
    override fun serialize(password: Password, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(password.value)
}

private object GroupChatTitleDeserializer : JsonDeserializer<GroupChatTitle>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): GroupChatTitle =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::GroupChatTitle)
}

private object GroupChatTitleSerializer : JsonSerializer<GroupChatTitle>() {
    override fun serialize(
        groupChatTitle: GroupChatTitle,
        generator: JsonGenerator,
        provider: SerializerProvider
    ): Unit = generator.writeString(groupChatTitle.value)
}

private object GroupChatDescriptionDeserializer : JsonDeserializer<GroupChatDescription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): GroupChatDescription =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::GroupChatDescription)
}

private object GroupChatDescriptionSerializer : JsonSerializer<GroupChatDescription>() {
    override fun serialize(
        groupChatDescription: GroupChatDescription,
        generator: JsonGenerator,
        provider: SerializerProvider
    ): Unit = generator.writeString(groupChatDescription.value)
}

private object MessageTextDeserializer : JsonDeserializer<MessageText>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): MessageText =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::MessageText)
}

private object MessageTextSerializer : JsonSerializer<MessageText>() {
    override fun serialize(messageText: MessageText, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(messageText.value)
}

private object BioDeserializer : JsonDeserializer<Bio>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Bio =
        parser.codec.readTree<JsonNode>(parser).textValue().let(::Bio)
}

private object BioSerializer : JsonSerializer<Bio>() {
    override fun serialize(bio: Bio, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(bio.value)
}

private object UuidDeserializer : JsonDeserializer<UUID>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): UUID =
        parser.codec.readTree<JsonNode>(parser).textValue().let(UUID::fromString)
}

private object UuidSerializer : JsonSerializer<UUID>() {
    override fun serialize(uuid: UUID, generator: JsonGenerator, provider: SerializerProvider): Unit =
        generator.writeString(uuid.toString())
}

/** Convenience function for [ObjectMapper.registerModule]. Registers the [T]'s [serializer] and [deserializer]. */
private inline fun <reified T : Any> ObjectMapper.register(
    deserializer: JsonDeserializer<T>,
    serializer: JsonSerializer<T>? = null
): ObjectMapper {
    val module = SimpleModule()
    module.addDeserializer(T::class.java, deserializer)
    serializer?.let { module.addSerializer(T::class.java, it) }
    return registerModule(module)
}