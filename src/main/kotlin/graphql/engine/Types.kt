package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.graphql.operations.GroupChatDto
import com.neelkamath.omniChat.graphql.operations.PrivateChatDto
import com.neelkamath.omniChat.graphql.routing.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun wireGraphQlTypes(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .type("AccountData") { wireType(it, ::readAccountData) }
    .type("Chat") { wireType(it, ::readChat) }
    .type("BareGroupChat") { wireType(it, ::readBareGroupChat) }
    .type("BareMessage") { wireType(it, ::readBareMessage) }
    .type("BareChatMessage") { wireType(it, ::readBareChatMessage) }
    .type("StarredMessage") { wireType(it, ::readStarredMessage) }
    .type("NewMessage") { wireType(it, ::readNewMessage) }
    .type("UpdatedMessage") { wireType(it, ::readUpdatedMessage) }
    .type("Message") { wireType(it, ::readMessage) }
    .type("MessagesSubscription") { wireType(it, ::readMessagesSubscription) }
    .type("OnlineStatusesSubscription") { wireType(it, ::readOnlineStatusesSubscription) }
    .type("TypingStatusesSubscription") { wireType(it, ::readTypingStatusesSubscription) }
    .type("AccountsSubscription") { wireType(it, ::readAccountsSubscription) }
    .type("GroupChatsSubscription") { wireType(it, ::readGroupChatsSubscription) }

private inline fun wireType(
    builder: TypeRuntimeWiring.Builder,
    crossinline reader: (Any) -> String,
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
    is NewActionMessage -> "NewActionMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewGroupChatInviteMessage -> "NewGroupChatInviteMessage"
    is NewDocMessage -> "NewDocMessage"
    is NewVideoMessage -> "NewVideoMessage"
    is NewPollMessage -> "NewPollMessage"
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedActionMessage -> "UpdatedActionMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    is UpdatedGroupChatInviteMessage -> "UpdatedGroupChatInviteMessage"
    is UpdatedDocMessage -> "UpdatedDocMessage"
    is UpdatedVideoMessage -> "UpdatedVideoMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is TriggeredAction -> "TriggeredAction"
    is DeletedMessage -> "DeletedMessage"
    is MessageDeletionPoint -> "MessageDeletionPoint"
    is DeletionOfEveryMessage -> "DeletionOfEveryMessage"
    is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readGroupChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is GroupChatId -> "GroupChatId"
    is UpdatedGroupChatPic -> "UpdatedGroupChatPic"
    is UpdatedGroupChat -> "UpdatedGroupChat"
    is ExitedUser -> "ExitedUser"
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

private fun readAccountsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewContact -> "NewContact"
    is UpdatedAccount -> "UpdatedAccount"
    is UpdatedProfilePic -> "UpdatedProfilePic"
    is DeletedContact -> "DeletedContact"
    is BlockedAccount -> "BlockedAccount"
    is UnblockedAccount -> "UnblockedAccount"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readAccountData(obj: Any): String = when (obj) {
    is Account -> "Account"
    is BlockedAccount -> "BlockedAccount"
    is NewContact -> "NewContact"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareGroupChat(obj: Any): String = when (obj) {
    is GroupChat -> "GroupChat"
    is GroupChatInfo -> "GroupChatInfo"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareMessage(obj: Any): String = when (obj) {
    is TextMessage -> "TextMessage"
    is ActionMessage -> "ActionMessage"
    is PicMessage -> "PicMessage"
    is PollMessage -> "PollMessage"
    is AudioMessage -> "AudioMessage"
    is GroupChatInviteMessage -> "GroupChatInviteMessage"
    is DocMessage -> "DocMessage"
    is VideoMessage -> "VideoMessage"
    is StarredTextMessage -> "StarredTextMessage"
    is StarredActionMessage -> "StarredActionMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    is StarredGroupChatInviteMessage -> "StarredGroupChatInviteMessage"
    is StarredDocMessage -> "StarredDocMessage"
    is StarredVideoMessage -> "StarredVideoMessage"
    is NewTextMessage -> "NewTextMessage"
    is NewActionMessage -> "NewActionMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewGroupChatInviteMessage -> "NewGroupChatInviteMessage"
    is NewDocMessage -> "NewDocMessage"
    is NewVideoMessage -> "NewVideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readMessage(obj: Any): String = when (obj) {
    is TextMessage -> "TextMessage"
    is ActionMessage -> "ActionMessage"
    is PicMessage -> "PicMessage"
    is PollMessage -> "PollMessage"
    is AudioMessage -> "AudioMessage"
    is GroupChatInviteMessage -> "GroupChatInviteMessage"
    is DocMessage -> "DocMessage"
    is VideoMessage -> "VideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareChatMessage(obj: Any): String = when (obj) {
    is StarredTextMessage -> "StarredTextMessage"
    is StarredActionMessage -> "StarredActionMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    is StarredGroupChatInviteMessage -> "StarredGroupChatInviteMessage"
    is StarredDocMessage -> "StarredDocMessage"
    is StarredVideoMessage -> "StarredVideoMessage"
    is NewTextMessage -> "NewTextMessage"
    is NewActionMessage -> "NewActionMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewGroupChatInviteMessage -> "NewGroupChatInviteMessage"
    is NewDocMessage -> "NewDocMessage"
    is NewVideoMessage -> "NewVideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readStarredMessage(obj: Any): String = when (obj) {
    is StarredTextMessage -> "StarredTextMessage"
    is StarredActionMessage -> "StarredActionMessage"
    is StarredPicMessage -> "StarredPicMessage"
    is StarredPollMessage -> "StarredPollMessage"
    is StarredAudioMessage -> "StarredAudioMessage"
    is StarredGroupChatInviteMessage -> "StarredGroupChatInviteMessage"
    is StarredDocMessage -> "StarredDocMessage"
    is StarredVideoMessage -> "StarredVideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readNewMessage(obj: Any): String = when (obj) {
    is NewTextMessage -> "NewTextMessage"
    is NewActionMessage -> "NewActionMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewPollMessage -> "NewPollMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewGroupChatInviteMessage -> "NewGroupChatInviteMessage"
    is NewDocMessage -> "NewDocMessage"
    is NewVideoMessage -> "NewVideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readUpdatedMessage(obj: Any): String = when (obj) {
    is UpdatedTextMessage -> "UpdatedTextMessage"
    is UpdatedActionMessage -> "UpdatedActionMessage"
    is UpdatedPicMessage -> "UpdatedPicMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is UpdatedAudioMessage -> "UpdatedAudioMessage"
    is UpdatedGroupChatInviteMessage -> "UpdatedGroupChatInviteMessage"
    is UpdatedDocMessage -> "UpdatedDocMessage"
    is UpdatedVideoMessage -> "UpdatedVideoMessage"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}
