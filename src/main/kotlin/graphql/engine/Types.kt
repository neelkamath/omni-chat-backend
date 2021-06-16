package com.neelkamath.omniChatBackend.graphql.engine

import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun wireGraphQlTypes(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .type("AccountData") { wireType(it, ::readAccountData) }
    .type("SetPublicityResult") { wireType(it, ::readSetPublicityResult) }
    .type("Chat") { wireType(it, ::readChat) }
    .type("BareGroupChat") { wireType(it, ::readBareGroupChat) }
    .type("StarredMessage") { wireType(it, ::readStarredMessage) }
    .type("NewMessage") { wireType(it, ::readNewMessage) }
    .type("Message") { wireType(it, ::readMessage) }
    .type("MessagesSubscription") { wireType(it, ::readMessagesSubscription) }
    .type("ChatMessagesSubscription") { wireType(it, ::readChatMessagesSubscription) }
    .type("OnlineStatusesSubscription") { wireType(it, ::readOnlineStatusesSubscription) }
    .type("ChatOnlineStatusesSubscription") { wireType(it, ::readChatOnlineStatusesSubscription) }
    .type("TypingStatusesSubscription") { wireType(it, ::readTypingStatusesSubscription) }
    .type("ChatTypingStatusesSubscription") { wireType(it, ::readChatTypingStatusesSubscription) }
    .type("AccountsSubscription") { wireType(it, ::readAccountsSubscription) }
    .type("ChatAccountsSubscription") { wireType(it, ::readChatAccountsSubscription) }
    .type("ChatsSubscription") { wireType(it, ::readChatsSubscription) }
    .type("GroupChatMetadataSubscription") { wireType(it, ::readGroupChatMetadataSubscription) }
    .type("SearchChatMessagesResult") { wireType(it, ::readSearchChatMessagesResult) }
    .type("ReadChatResult") { wireType(it, ::readReadChatResult) }
    .type("RemoveGroupChatUsersResult") { wireType(it, ::readRemoveGroupChatUsersResult) }
    .type("ReadGroupChatResult") { wireType(it, ::readReadGroupChatResult) }
    .type("RequestTokenSetResult") { wireType(it, ::readRequestTokenSetResult) }
    .type("VerifyEmailAddressResult") { wireType(it, ::readVerifyEmailAddressResult) }
    .type("ResetPasswordResult") { wireType(it, ::readResetPasswordResult) }
    .type("UpdateAccountResult") { wireType(it, ::readUpdateAccountResult) }
    .type("CreateAccountResult") { wireType(it, ::readCreateAccountResult) }
    .type("ReadMessageResult") { wireType(it, ::readReadMessageResult) }
    .type("SearchGroupChatUsersResult") { wireType(it, ::readSearchGroupChatUsersResult) }
    .type("EmailEmailAddressVerificationResult") { wireType(it, ::readEmailEmailAddressVerificationResult) }
    .type("CreateGroupChatResult") { wireType(it, ::readCreateGroupChatResult) }
    .type("CreatePrivateChatResult") { wireType(it, ::readCreatePrivateChatResult) }
    .type("CreateTextMessageResult") { wireType(it, ::readCreateTextMessageResult) }
    .type("CreateActionMessageResult") { wireType(it, ::readCreateActionMessageResult) }
    .type("CreateGroupChatInviteMessageResult") { wireType(it, ::readCreateGroupChatInviteMessageResult) }
    .type("CreatePollMessageResult") { wireType(it, ::readCreatePollMessageResult) }
    .type("ForwardMessageResult") { wireType(it, ::readForwardMessageResult) }
    .type("TriggerActionResult") { wireType(it, ::readTriggerActionResult) }
    .type("SetPollVoteResult") { wireType(it, ::readSetPollVoteResult) }
    .type("LeaveGroupChatResult") { wireType(it, ::readLeaveGroupChatResult) }
    .type("ReadOnlineStatusResult") { wireType(it, ::readReadOnlineStatusResult) }

private inline fun wireType(
    builder: TypeRuntimeWiring.Builder,
    crossinline reader: (Any) -> String,
): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = reader(it.getObject())
    it.schema.getObjectType(type)
}

private fun readChat(obj: Any): String = when (obj) {
    is PrivateChat -> "PrivateChat"
    is GroupChat -> "GroupChat"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readChatMessagesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewTextMessage -> "NewTextMessage"
    is NewActionMessage -> "NewActionMessage"
    is NewPicMessage -> "NewPicMessage"
    is NewAudioMessage -> "NewAudioMessage"
    is NewGroupChatInviteMessage -> "NewGroupChatInviteMessage"
    is NewDocMessage -> "NewDocMessage"
    is NewVideoMessage -> "NewVideoMessage"
    is NewPollMessage -> "NewPollMessage"
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is UpdatedMessage -> "UpdatedMessage"
    is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
    is InvalidChatId -> "InvalidChatId"
    is DeletedMessage -> "DeletedMessage"
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
    is UpdatedPollMessage -> "UpdatedPollMessage"
    is UpdatedMessage -> "UpdatedMessage"
    is TriggeredAction -> "TriggeredAction"
    is DeletedMessage -> "DeletedMessage"
    is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is GroupChatId -> "GroupChatId"
    is UpdatedGroupChatPic -> "UpdatedGroupChatPic"
    is UpdatedGroupChat -> "UpdatedGroupChat"
    is DeletedPrivateChat -> "DeletedPrivateChat"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readGroupChatMetadataSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedGroupChatPic -> "UpdatedGroupChatPic"
    is UpdatedGroupChat -> "UpdatedGroupChat"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readSearchChatMessagesResult(obj: Any): String = when (obj) {
    is MessageEdges -> "MessageEdges"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readReadChatResult(obj: Any): String = when (obj) {
    is PrivateChat -> "PrivateChat"
    is GroupChat -> "GroupChat"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readRemoveGroupChatUsersResult(obj: Any): String = when (obj) {
    is CannotLeaveChat -> "CannotLeaveChat"
    is MustBeAdmin -> "MustBeAdmin"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readReadGroupChatResult(obj: Any): String = when (obj) {
    is GroupChatInfo -> "GroupChatInfo"
    is InvalidInviteCode -> "InvalidInviteCode"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readRequestTokenSetResult(obj: Any): String = when (obj) {
    is TokenSet -> "TokenSet"
    is NonexistingUser -> "NonexistingUser"
    is UnverifiedEmailAddress -> "UnverifiedEmailAddress"
    is IncorrectPassword -> "IncorrectPassword"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readVerifyEmailAddressResult(obj: Any): String = when (obj) {
    is InvalidVerificationCode -> "InvalidVerificationCode"
    is UnregisteredEmailAddress -> "UnregisteredEmailAddress"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readResetPasswordResult(obj: Any): String = when (obj) {
    is InvalidPasswordResetCode -> "InvalidPasswordResetCode"
    is UnregisteredEmailAddress -> "UnregisteredEmailAddress"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readUpdateAccountResult(obj: Any): String = when (obj) {
    is UsernameTaken -> "UsernameTaken"
    is EmailAddressTaken -> "EmailAddressTaken"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreateAccountResult(obj: Any): String = when (obj) {
    is UsernameTaken -> "UsernameTaken"
    is EmailAddressTaken -> "EmailAddressTaken"
    is InvalidDomain -> "InvalidDomain"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readReadMessageResult(obj: Any): String = when (obj) {
    is InvalidMessageId -> "InvalidMessageId"
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

private fun readSearchGroupChatUsersResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is AccountsConnection -> "AccountsConnection"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readEmailEmailAddressVerificationResult(obj: Any): String = when (obj) {
    is UnregisteredEmailAddress -> "UnregisteredEmailAddress"
    is EmailAddressVerified -> "EmailAddressVerified"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreateGroupChatResult(obj: Any): String = when (obj) {
    is CreatedChatId -> "CreatedChatId"
    is InvalidAdminId -> "InvalidAdminId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreatePrivateChatResult(obj: Any): String = when (obj) {
    is CreatedChatId -> "CreatedChatId"
    is InvalidUserId -> "InvalidUserId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreateTextMessageResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is InvalidMessageId -> "InvalidMessageId"
    is MustBeAdmin -> "MustBeAdmin"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreateActionMessageResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is InvalidAction -> "InvalidAction"
    is InvalidMessageId -> "InvalidMessageId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreateGroupChatInviteMessageResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is InvalidInvitedChat -> "InvalidInvitedChat"
    is InvalidMessageId -> "InvalidMessageId"
    is MustBeAdmin -> "MustBeAdmin"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readCreatePollMessageResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is InvalidMessageId -> "InvalidMessageId"
    is InvalidPoll -> "InvalidPoll"
    is MustBeAdmin -> "MustBeAdmin"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readForwardMessageResult(obj: Any): String = when (obj) {
    is MustBeAdmin -> "MustBeAdmin"
    is InvalidChatId -> "InvalidChatId"
    is InvalidMessageId -> "InvalidMessageId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readTriggerActionResult(obj: Any): String = when (obj) {
    is InvalidMessageId -> "InvalidMessageId"
    is InvalidAction -> "InvalidAction"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readLeaveGroupChatResult(obj: Any): String = when (obj) {
    is InvalidChatId -> "InvalidChatId"
    is CannotLeaveChat -> "CannotLeaveChat"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readReadOnlineStatusResult(obj: Any): String = when (obj) {
    is InvalidUserId -> "InvalidUserId"
    is OnlineStatus -> "OnlineStatus"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readSetPollVoteResult(obj: Any): String = when (obj) {
    is InvalidMessageId -> "InvalidMessageId"
    is NonexistingOption -> "NonexistingOption"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readTypingStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is TypingStatus -> "TypingStatus"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readChatTypingStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is TypingStatus -> "TypingStatus"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readOnlineStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is OnlineStatus -> "OnlineStatus"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readChatOnlineStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is OnlineStatus -> "OnlineStatus"
    is InvalidChatId -> "InvalidChatId"
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
    is DeletedAccount -> "DeletedAccount"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readChatAccountsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedAccount -> "UpdatedAccount"
    is UpdatedProfilePic -> "UpdatedProfilePic"
    is DeletedAccount -> "DeletedAccount"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readAccountData(obj: Any): String = when (obj) {
    is Account -> "Account"
    is BlockedAccount -> "BlockedAccount"
    is NewContact -> "NewContact"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readSetPublicityResult(obj: Any): String = when (obj) {
    is MustBeAdmin -> "MustBeAdmin"
    is InvalidChatId -> "InvalidChatId"
    else -> throw IllegalArgumentException("$obj didn't map to a concrete type.")
}

private fun readBareGroupChat(obj: Any): String = when (obj) {
    is GroupChat -> "GroupChat"
    is GroupChatInfo -> "GroupChatInfo"
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
