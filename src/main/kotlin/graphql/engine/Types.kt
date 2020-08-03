package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.graphql.operations.GroupChatDto
import com.neelkamath.omniChat.graphql.operations.PrivateChatDto
import com.neelkamath.omniChat.graphql.routing.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun wireGraphQlTypes(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .type("MessagesSubscription") { wireType(it, ::readMessagesSubscription) }
    .type("ContactsSubscription") { wireType(it, ::readContactsSubscription) }
    .type("UpdatedChatsSubscription") { wireType(it, ::readUpdatedChatsSubscription) }
    .type("NewGroupChatsSubscription") { wireType(it, ::readNewGroupChatsSubscription) }
    .type("TypingStatusesSubscription") { wireType(it, ::readTypingStatusesSubscription) }
    .type("OnlineStatusesSubscription") { wireType(it, ::readOnlineStatusesSubscription) }
    .type("Chat") { wireType(it, ::readChat) }
    .type("AccountData") { wireType(it, ::readAccountData) }
    .type("BareMessage") { wireType(it, ::readBareMessage) }
    .type("Message") { wireType(it, ::readMessage) }
    .type("BareChatMessage") { wireType(it, ::readBareChatMessage) }
    .type("StarredMessage") { wireType(it, ::readStarredMessage) }
    .type("NewMessage") { wireType(it, ::readNewMessage) }
    .type("UpdatedMessage") { wireType(it, ::readUpdatedMessage) }

private inline fun wireType(
    builder: TypeRuntimeWiring.Builder,
    crossinline reader: (Any) -> String
): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = reader(it.getObject())
    it.schema.getObjectType(type)
}

private fun readChat(obj: Any): String = when (obj) {
    is PrivateChat, is PrivateChatDto -> "PrivateChat"
    is GroupChat, is GroupChatDto -> "GroupChat"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readMessagesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewTextMessage -> "NewTextMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewPollMessage -> "NewPollMessage"
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is DeletedMessage -> "DeletedMessage"
    is MessageDeletionPoint -> "MessageDeletionPoint"
    is DeletionOfEveryMessage -> "DeletionOfEveryMessage"
    is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readUpdatedChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedGroupChat -> "UpdatedGroupChat"
    is UpdatedAccount -> "UpdatedAccount"
    is ExitedUser -> "ExitedUser"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readNewGroupChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is GroupChatId -> "GroupChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readTypingStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is TypingStatus -> "TypingStatus"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readOnlineStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedOnlineStatus -> "UpdatedOnlineStatus"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readContactsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewContact -> "NewContact"
    is UpdatedContact -> "UpdatedContact"
    is DeletedContact -> "DeletedContact"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readAccountData(obj: Any): String = when (obj) {
    is Account -> "Account"
    is UpdatedContact -> "UpdatedContact"
    is NewContact -> "NewContact"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareMessage(obj: Any): String = when (obj) {
    is TextMessage -> "TextMessage"
    is PicMessage -> "PicMessage"
    is PollMessage -> "PollMessage"
    is AudioMessage -> "AudioMessage"
    is StarredTextMessage -> "StarredTextMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    is NewTextMessage -> "NewTextMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readMessage(obj: Any): String = when (obj) {
    is TextMessage -> "TextMessage"
    is PicMessage -> "PicMessage"
    is PollMessage -> "PollMessage"
    is AudioMessage -> "AudioMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareChatMessage(obj: Any): String = when (obj) {
    is StarredTextMessage -> "StarredTextMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    is NewTextMessage -> "NewTextMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readStarredMessage(obj: Any): String = when (obj) {
    is StarredTextMessage -> "StarredTextMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readNewMessage(obj: Any): String = when (obj) {
    is NewTextMessage -> "NewTextMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readUpdatedMessage(obj: Any): String = when (obj) {
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}