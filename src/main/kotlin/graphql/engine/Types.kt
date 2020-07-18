package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.operations.GroupChatDto
import com.neelkamath.omniChat.graphql.operations.PrivateChatDto
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
    .type("MessageData") { wireType(it, ::readMessageData) }

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
    else -> throw IllegalArgumentException("$obj wasn't a PrivateChat, PrivateChatDto, GroupChat, or GroupChatDto.")
}

private fun readMessagesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewMessage -> "NewMessage"
    is UpdatedMessage -> "UpdatedMessage"
    is DeletedMessage -> "DeletedMessage"
    is MessageDeletionPoint -> "MessageDeletionPoint"
    is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
    is DeletionOfEveryMessage -> "DeletionOfEveryMessage"
    else -> throw IllegalArgumentException(
        """
        $obj wasn't a CreatedSubscription, NewMessage, UpdatedMessage, DeletedMessage, MessageDeletionPoint,
        UserChatMessagesRemoval, or DeletionOfEveryMessage.
        """.trimIndent()
    )
}

private fun readUpdatedChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedGroupChat -> "UpdatedGroupChat"
    is UpdatedAccount -> "UpdatedAccount"
    is ExitedUser -> "ExitedUser"
    else -> throw IllegalArgumentException(
        "$obj wasn't a CreatedSubscription, UpdatedGroupChat, UpdatedAccount, or ExitedUser."
    )
}

private fun readNewGroupChatsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is GroupChatId -> "GroupChatId"
    else -> throw IllegalArgumentException("$obj wasn't a CreatedSubscription or GroupChat.")
}

private fun readTypingStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is TypingStatus -> "TypingStatus"
    else -> throw IllegalArgumentException("$obj wasn't a CreatedSubscription or TypingStatus.")
}

private fun readOnlineStatusesSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is UpdatedOnlineStatus -> "UpdatedOnlineStatus"
    else -> throw IllegalArgumentException("$obj wasn't a CreatedSubscription or UpdatedOnlineStatus.")
}

private fun readContactsSubscription(obj: Any): String = when (obj) {
    is CreatedSubscription -> "CreatedSubscription"
    is NewContact -> "NewContact"
    is UpdatedContact -> "UpdatedContact"
    is DeletedContact -> "DeletedContact"
    else -> throw IllegalArgumentException(
        "$obj wasn't a CreatedSubscription, NewContact, UpdatedContact, or DeletedContact."
    )
}

private fun readAccountData(obj: Any): String = when (obj) {
    is Account -> "Account"
    is UpdatedContact -> "UpdatedContact"
    is DeletedContact -> "DeletedContact"
    is NewContact -> "NewContact"
    else -> throw IllegalArgumentException("$obj wasn't an Account, UpdatedContact, DeletedContact, or NewContact.")
}

private fun readMessageData(obj: Any): String = when (obj) {
    is NewMessage -> "NewMessage"
    is UpdatedMessage -> "UpdatedMessage"
    else -> throw IllegalArgumentException("$obj wasn't a NewMessage, or UpdatedMessage.")
}