package com.neelkamath.omniChatBackend

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TokenSet
import com.neelkamath.omniChatBackend.graphql.routing.*
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    .register(MessageDeserializer)
    .register(StarredMessageDeserializer)
    .register(NewMessageDeserializer)
    .register(UsernameDeserializer, UsernameSerializer)
    .register(NameDeserializer, NameSerializer)
    .register(PasswordDeserializer, PasswordSerializer)
    .register(GroupChatTitleDeserializer, GroupChatTitleSerializer)
    .register(GroupChatDescriptionDeserializer, GroupChatDescriptionSerializer)
    .register(MessageTextDeserializer, MessageTextSerializer)
    .register(BioDeserializer, BioSerializer)
    .register(UuidDeserializer, UuidSerializer)
    .register(OnlineStatusesSubscriptionDeserializer)
    .register(ChatOnlineStatusesSubscriptionDeserializer)
    .register(MessagesSubscriptionDeserializer)
    .register(ChatMessagesSubscriptionDeserializer)
    .register(SearchChatMessagesResultDeserializer)
    .register(ReadChatResultDeserializer)
    .register(CreateActionMessageResultDeserializer)
    .register(CreateGroupChatInviteMessageResultDeserializer)
    .register(RequestTokenSetResultDeserializer)
    .register(ReadGroupChatResultDeserializer)
    .register(VerifyEmailAddressResultDeserializer)
    .register(ResetPasswordResultDeserializer)
    .register(UpdateAccountResultDeserializer)
    .register(CreateAccountResultDeserializer)
    .register(CreateGroupChatResultDeserializer)
    .register(CreatePrivateChatResultDeserializer)
    .register(CreateTextMessageResultDeserializer)
    .register(CreatePollMessageResultDeserializer)
    .register(ForwardMessageResultDeserializer)
    .register(SetPollVoteResultDeserializer)
    .register(LeaveGroupChatResultDeserializer)
    .register(ReadOnlineStatusResultDeserializer)
    .register(EmailEmailAddressVerificationResultDeserializer)

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
            "OnlineStatus" -> OnlineStatus::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ChatOnlineStatusesSubscriptionDeserializer : JsonDeserializer<ChatOnlineStatusesSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ChatOnlineStatusesSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "OnlineStatus" -> OnlineStatus::class
            "InvalidChatId" -> InvalidChatId::class
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
            "ExitedUsers" -> ExitedUsers::class
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
            "DeletedAccount" -> DeletedAccount::class
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
            "UpdatedMessage" -> UpdatedMessage::class
            "TriggeredAction" -> TriggeredAction::class
            "DeletedMessage" -> DeletedMessage::class
            "UserChatMessagesRemoval" -> UserChatMessagesRemoval::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ChatMessagesSubscriptionDeserializer : JsonDeserializer<ChatMessagesSubscription>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ChatMessagesSubscription {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ChatMessagesSubscription> = when (val type = node["__typename"].asText()) {
            "CreatedSubscription" -> CreatedSubscription::class
            "NewTextMessage" -> NewTextMessage::class
            "NewActionMessage" -> NewActionMessage::class
            "NewPicMessage" -> NewPicMessage::class
            "NewPollMessage" -> NewPollMessage::class
            "NewAudioMessage" -> NewAudioMessage::class
            "NewGroupChatInviteMessage" -> NewGroupChatInviteMessage::class
            "NewDocMessage" -> NewDocMessage::class
            "NewVideoMessage" -> NewVideoMessage::class
            "UpdatedMessage" -> UpdatedMessage::class
            "DeletedMessage" -> DeletedMessage::class
            "UserChatMessagesRemoval" -> UserChatMessagesRemoval::class
            "InvalidChatId" -> InvalidChatId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object SearchChatMessagesResultDeserializer : JsonDeserializer<SearchChatMessagesResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): SearchChatMessagesResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out SearchChatMessagesResult> = when (val type = node["__typename"].asText()) {
            "MessageEdges" -> MessageEdges::class
            "InvalidChatId" -> InvalidChatId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ReadChatResultDeserializer : JsonDeserializer<ReadChatResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ReadChatResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ReadChatResult> = when (val type = node["__typename"].asText()) {
            "PrivateChat" -> PrivateChat::class
            "GroupChat" -> GroupChat::class
            "InvalidChatId" -> InvalidChatId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object RequestTokenSetResultDeserializer : JsonDeserializer<RequestTokenSetResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): RequestTokenSetResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out RequestTokenSetResult> = when (val type = node["__typename"].asText()) {
            "TokenSet" -> TokenSet::class
            "NonexistingUser" -> NonexistingUser::class
            "UnverifiedEmailAddress" -> UnverifiedEmailAddress::class
            "IncorrectPassword" -> IncorrectPassword::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ReadGroupChatResultDeserializer : JsonDeserializer<ReadGroupChatResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ReadGroupChatResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ReadGroupChatResult> = when (val type = node["__typename"].asText()) {
            "GroupChatInfo" -> GroupChatInfo::class
            "InvalidInviteCode" -> InvalidInviteCode::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object VerifyEmailAddressResultDeserializer : JsonDeserializer<VerifyEmailAddressResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): VerifyEmailAddressResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out VerifyEmailAddressResult> = when (val type = node["__typename"].asText()) {
            "InvalidVerificationCode" -> InvalidVerificationCode::class
            "UnregisteredEmailAddress" -> UnregisteredEmailAddress::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ResetPasswordResultDeserializer : JsonDeserializer<ResetPasswordResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ResetPasswordResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ResetPasswordResult> = when (val type = node["__typename"].asText()) {
            "InvalidPasswordResetCode" -> InvalidPasswordResetCode::class
            "UnregisteredEmailAddress" -> UnregisteredEmailAddress::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object UpdateAccountResultDeserializer : JsonDeserializer<UpdateAccountResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): UpdateAccountResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out UpdateAccountResult> = when (val type = node["__typename"].asText()) {
            "UsernameTaken" -> UsernameTaken::class
            "EmailAddressTaken" -> EmailAddressTaken::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreateAccountResultDeserializer : JsonDeserializer<CreateAccountResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreateAccountResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreateAccountResult> = when (val type = node["__typename"].asText()) {
            "UsernameTaken" -> UsernameTaken::class
            "EmailAddressTaken" -> EmailAddressTaken::class
            "InvalidDomain" -> InvalidDomain::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreateGroupChatResultDeserializer : JsonDeserializer<CreateGroupChatResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreateGroupChatResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreateGroupChatResult> = when (val type = node["__typename"].asText()) {
            "CreatedChatId" -> CreatedChatId::class
            "InvalidAdminId" -> InvalidAdminId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreatePrivateChatResultDeserializer : JsonDeserializer<CreatePrivateChatResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreatePrivateChatResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreatePrivateChatResult> = when (val type = node["__typename"].asText()) {
            "CreatedChatId" -> CreatedChatId::class
            "InvalidUserId" -> InvalidUserId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreateTextMessageResultDeserializer : JsonDeserializer<CreateTextMessageResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreateTextMessageResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreateTextMessageResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "InvalidMessageId" -> InvalidMessageId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreatePollMessageResultDeserializer : JsonDeserializer<CreatePollMessageResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreatePollMessageResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreatePollMessageResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "InvalidMessageId" -> InvalidMessageId::class
            "InvalidPoll" -> InvalidPoll::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ForwardMessageResultDeserializer : JsonDeserializer<ForwardMessageResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ForwardMessageResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ForwardMessageResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "InvalidMessageId" -> InvalidMessageId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object SetPollVoteResultDeserializer : JsonDeserializer<SetPollVoteResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): SetPollVoteResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out SetPollVoteResult> = when (val type = node["__typename"].asText()) {
            "NonexistingOption" -> NonexistingOption::class
            "InvalidMessageId" -> InvalidMessageId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object LeaveGroupChatResultDeserializer : JsonDeserializer<LeaveGroupChatResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): LeaveGroupChatResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out LeaveGroupChatResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "CannotLeaveChat" -> CannotLeaveChat::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object ReadOnlineStatusResultDeserializer : JsonDeserializer<ReadOnlineStatusResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ReadOnlineStatusResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out ReadOnlineStatusResult> = when (val type = node["__typename"].asText()) {
            "OnlineStatus" -> OnlineStatus::class
            "InvalidUserId" -> InvalidUserId::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreateActionMessageResultDeserializer : JsonDeserializer<CreateActionMessageResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreateActionMessageResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreateActionMessageResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "InvalidMessageId" -> InvalidMessageId::class
            "InvalidAction" -> InvalidAction::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object CreateGroupChatInviteMessageResultDeserializer : JsonDeserializer<CreateGroupChatInviteMessageResult>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): CreateGroupChatInviteMessageResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out CreateGroupChatInviteMessageResult> = when (val type = node["__typename"].asText()) {
            "InvalidChatId" -> InvalidChatId::class
            "InvalidMessageId" -> InvalidMessageId::class
            "InvalidInvitedChat" -> InvalidInvitedChat::class
            else -> throw IllegalArgumentException("$type didn't match a concrete class.")
        }
        return parser.codec.treeToValue(node, clazz.java)
    }
}

private object EmailEmailAddressVerificationResultDeserializer :
    JsonDeserializer<EmailEmailAddressVerificationResult>() {

    override fun deserialize(parser: JsonParser, context: DeserializationContext): EmailEmailAddressVerificationResult {
        val node = parser.codec.readTree<JsonNode>(parser)
        val clazz: KClass<out EmailEmailAddressVerificationResult> = when (val type = node["__typename"].asText()) {
            "UnregisteredEmailAddress" -> UnregisteredEmailAddress::class
            "EmailAddressVerified" -> EmailAddressVerified::class
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
        provider: SerializerProvider,
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
        provider: SerializerProvider,
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
    serializer: JsonSerializer<T>? = null,
): ObjectMapper {
    val module = SimpleModule()
    module.addDeserializer(T::class.java, deserializer)
    serializer?.let { module.addSerializer(T::class.java, it) }
    return registerModule(module)
}


@ExtendWith(DbExtension::class)
class AppTest {
    @Nested
    @Suppress("ClassName")
    inner class Application_Main {
        private fun executeReadChats(accessToken: String): HttpStatusCode = executeGraphQlViaHttp(
            """
            query ReadChats {
                readChats
            }
            """,
            accessToken = accessToken,
        ).status()!!

        @Test
        fun `An access token must work for queries and mutations`() {
            val userId = createVerifiedUsers(1).first().userId
            val token = buildTokenSet(userId).accessToken.value
            assertEquals(HttpStatusCode.OK, executeReadChats(token))
        }

        @Test
        fun `A token from an account with an unverified email address mustn't work for queries and mutation`() {
            val userId = createVerifiedUsers(1).first().userId
            val token = buildTokenSet(userId).accessToken.value
            Users.update(userId, AccountUpdate(emailAddress = "new.address@example.com"))
            assertEquals(HttpStatusCode.Unauthorized, executeReadChats(token))
        }
    }
}

/**
 * Sanity tests for encoding.
 *
 * Although encoding issues may seem to not be a problem these days,
 * https://github.com/graphql-java/graphql-java/issues/1877 shows otherwise. Had these tests not existed, such an
 * encoding problem may not have been found until technical debt from the tool at fault had already accumulated.
 */
@ExtendWith(DbExtension::class)
class EncodingTest {
    private fun executeCreateTextMessage(accessToken: String, chatId: Int, message: MessageText) {
        executeGraphQlViaHttp(
            """
            mutation CreateTextMessage(${"$"}chatId: Int!, ${"$"}text: MessageText!) {
                createTextMessage(chatId: ${"$"}chatId, text: ${"$"}text) {
                    __typename
                }
            }
            """,
            mapOf("chatId" to chatId, "text" to message),
            accessToken,
        )
    }

    @Test
    fun `A message must allow using emoji and multiple languages`() {
        val admin = createVerifiedUsers(1).first()
        val chatId = GroupChats.create(listOf(admin.userId))
        val message = MessageText("Emoji: \uD83D\uDCDA Japanese: 日 Chinese: 传/傳 Kannada: ಘ")
        executeCreateTextMessage(admin.accessToken, chatId, message)
        val messageId = Messages.readGroupChat(chatId).first()
        assertEquals(message, TextMessages.read(messageId))
    }
}

/**
 * These tests verify that the data the client receives conforms to the GraphQL spec.
 *
 * We use a GraphQL library which returns data according to the spec. However, there are two caveats which can cause the
 * data the client receives to be non-compliant with the spec:
 * 1. The GraphQL library returns `null` values for the `"data"` and `"errors"` keys. The spec mandates that these keys
 * keys either be non-`null` or not be returned at all.
 * 1. Data is serialized as JSON using the [testingObjectMapper]. The [testingObjectMapper] which may remove `null`
 * fields if incorrectly configured. The spec mandates that requested fields be returned even if they're `null`.
 */
@ExtendWith(DbExtension::class)
class SpecComplianceTest {
    private fun executeRequestTokenSet(login: Any): Map<String, Any> = readGraphQlHttpResponse(
        """
        query RequestTokenSet(${"$"}login: Login!) {
            requestTokenSet(login: ${"$"}login) {
                __typename
            }
        }
        """,
        mapOf("login" to login),
    )

    @Test
    fun `The data key mustn't be returned if there was no data to be received`(): Unit =
        assertTrue("data" !in executeRequestTokenSet(login = "invalid data").keys)

    @Test
    fun `The errors key mustn't be returned if there were no errors`() {
        val login = createVerifiedUsers(1).first().login
        assertTrue("errors" !in executeRequestTokenSet(login).keys)
    }

    @Test
    fun `'null' fields in the data key must be returned`() {
        val admin = createVerifiedUsers(1).first()
        val chatId = GroupChats.create(listOf(admin.userId))
        val messageId = Messages.message(admin.userId, chatId)
        val response = readGraphQlHttpResponse(
            """
            query ReadMessage(${"$"}messageId: Int!) {
                readMessage(messageId: ${"$"}messageId) {
                    context {
                        messageId
                    }
                }
            }
            """,
            mapOf("messageId" to messageId),
            admin.accessToken,
        )["data"] as Map<*, *>
        val data = response["readMessage"] as Map<*, *>
        val context = data["context"] as Map<*, *>
        assertContains(context.values, null)
    }
}
