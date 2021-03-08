package com.neelkamath.omniChat

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChat.graphql.routing.*
import java.util.*
import kotlin.reflect.KClass

/** The [objectMapper] for the test source set. */
val testingObjectMapper: ObjectMapper = objectMapper
    .copy()
    .register(PlaceholderDeserializer)
    .register(TypingStatusesSubscriptionDeserializer)
    .register(ChatDeserializer)
    .register(BareGroupChatDeserializer)
    .register(AccountsSubscriptionDeserializer)
    .register(GroupChatsSubscriptionDeserializer)
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
    .register(OnlineStatusesSubscriptionDeserializer)
    .register(MessagesSubscriptionDeserializer)

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
            "BlockedAccount" -> BlockedAccount::class
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
            "ActionMessage" -> ActionMessage::class
            "PicMessage" -> PicMessage::class
            "PollMessage" -> PollMessage::class
            "AudioMessage" -> AudioMessage::class
            "GroupChatInviteMessage" -> GroupChatInviteMessage::class
            "DocMessage" -> DocMessage::class
            "VideoMessage" -> VideoMessage::class
            "StarredTextMessage" -> StarredTextMessage::class
            "StarredActionMessage" -> StarredActionMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            "StarredGroupChatInviteMessage" -> StarredGroupChatInviteMessage::class
            "StarredDocMessage" -> StarredDocMessage::class
            "StarredVideoMessage" -> StarredVideoMessage::class
            "NewTextMessage" -> NewTextMessage::class
            "NewActionMessage" -> NewActionMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "NewGroupChatInviteMessage" -> NewGroupChatInviteMessage::class
            "NewDocMessage" -> NewDocMessage::class
            "NewVideoMessage" -> NewVideoMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object BareGroupChatDeserializer : JsonDeserializer<BareGroupChat>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): BareGroupChat {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "GroupChat" -> GroupChat::class
            "GroupChatInfo" -> GroupChatInfo::class
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
            "ActionMessage" -> ActionMessage::class
            "PicMessage" -> PicMessage::class
            "PollMessage" -> PollMessage::class
            "AudioMessage" -> AudioMessage::class
            "GroupChatInviteMessage" -> GroupChatInviteMessage::class
            "DocMessage" -> DocMessage::class
            "VideoMessage" -> VideoMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object TypingStatusesSubscriptionDeserializer : JsonDeserializer<TypingStatusesSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): TypingStatusesSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "TypingStatus" -> TypingStatus::class
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
            "StarredActionMessage" -> StarredActionMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            "StarredGroupChatInviteMessage" -> StarredGroupChatInviteMessage::class
            "StarredDocMessage" -> StarredDocMessage::class
            "StarredVideoMessage" -> StarredVideoMessage::class
            "NewTextMessage" -> NewTextMessage::class
            "NewActionMessage" -> NewActionMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "NewGroupChatInviteMessage" -> NewGroupChatInviteMessage::class
            "NewDocMessage" -> NewDocMessage::class
            "NewVideoMessage" -> NewVideoMessage::class
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
            "StarredActionMessage" -> StarredActionMessage::class
            "StarredPicMessage" -> StarredPicMessage::class
            "StarredPollMessage" -> StarredPollMessage::class
            "StarredAudioMessage" -> StarredAudioMessage::class
            "StarredGroupChatInviteMessage" -> StarredGroupChatInviteMessage::class
            "StarredDocMessage" -> StarredDocMessage::class
            "StarredVideoMessage" -> StarredVideoMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object OnlineStatusesSubscriptionDeserializer : JsonDeserializer<OnlineStatusesSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): OnlineStatusesSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "UpdatedOnlineStatus" -> UpdatedOnlineStatus::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object GroupChatsSubscriptionDeserializer : JsonDeserializer<GroupChatsSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): GroupChatsSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "GroupChatId" -> GroupChatId::class
            "UpdatedGroupChatPic" -> UpdatedGroupChatPic::class
            "UpdatedGroupChat" -> UpdatedGroupChat::class
            "ExitedUser" -> ExitedUser::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object AccountsSubscriptionDeserializer : JsonDeserializer<AccountsSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): AccountsSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "NewContact" -> NewContact::class
            "UpdatedAccount" -> UpdatedAccount::class
            "UpdatedProfilePic" -> UpdatedProfilePic::class
            "DeletedContact" -> DeletedContact::class
            "BlockedAccount" -> BlockedAccount::class
            "UnblockedAccount" -> UnblockedAccount::class
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
            "NewActionMessage" -> NewActionMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "NewGroupChatInviteMessage" -> NewGroupChatInviteMessage::class
            "NewDocMessage" -> NewDocMessage::class
            "NewVideoMessage" -> NewVideoMessage::class
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
            "UpdatedActionMessage" -> UpdatedActionMessage::class
            "UpdatedPicMessage" -> UpdatedPicMessage::class
            "UpdatedPollMessage" -> UpdatedPollMessage::class
            "UpdatedAudioMessage" -> UpdatedAudioMessage::class
            "UpdatedGroupChatInviteMessage" -> UpdatedGroupChatInviteMessage::class
            "UpdatedDocMessage" -> UpdatedDocMessage::class
            "UpdatedVideoMessage" -> UpdatedVideoMessage::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object MessagesSubscriptionDeserializer : JsonDeserializer<MessagesSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): MessagesSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out MessagesSubscription> = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "NewTextMessage" -> NewTextMessage::class
            "NewActionMessage" -> NewActionMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "NewGroupChatInviteMessage" -> NewGroupChatInviteMessage::class
            "NewDocMessage" -> NewDocMessage::class
            "NewVideoMessage" -> NewVideoMessage::class
            "UpdatedTextMessage" -> UpdatedTextMessage::class
            "UpdatedActionMessage" -> UpdatedActionMessage::class
            "UpdatedPicMessage" -> UpdatedPicMessage::class
            "UpdatedPollMessage" -> UpdatedPollMessage::class
            "UpdatedAudioMessage" -> UpdatedAudioMessage::class
            "UpdatedGroupChatInviteMessage" -> UpdatedGroupChatInviteMessage::class
            "UpdatedDocMessage" -> UpdatedDocMessage::class
            "UpdatedVideoMessage" -> UpdatedVideoMessage::class
            "TriggeredAction" -> TriggeredAction::class
            "DeletedMessage" -> DeletedMessage::class
            "MessageDeletionPoint" -> MessageDeletionPoint::class
            "DeletionOfEveryMessage" -> DeletionOfEveryMessage::class
            "UserChatMessagesRemoval" -> UserChatMessagesRemoval::class
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
