package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.operations.GroupChatDto
import com.neelkamath.omniChat.graphql.operations.PrivateChatDto
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

fun wireGraphQlTypes(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder
    .type("MessagesSubscription", ::wireMessagesSubscription)
    .type("ContactsSubscription", ::wireContactsSubscription)
    .type("PrivateChatInfoSubscription", ::wirePrivateChatInfoSubscription)
    .type("GroupChatInfoSubscription", ::wireGroupChatInfoSubscription)
    .type("Chat", ::wireChat)
    .type("AccountData", ::wireAccountData)
    .type("MessageData", ::wireMessageData)

private fun wireChat(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = when (val obj = it.getObject<Any>()) {
        is PrivateChat, is PrivateChatDto -> "PrivateChat"
        is GroupChat, is GroupChatDto -> "GroupChat"
        else -> throw Error("$obj was neither a PrivateChat nor a GroupChat.")
    }
    it.schema.getObjectType(type)
}

private fun wireMessagesSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is NewMessage -> "NewMessage"
            is UpdatedMessage -> "UpdatedMessage"
            is DeletedMessage -> "DeletedMessage"
            is MessageDeletionPoint -> "MessageDeletionPoint"
            is UserChatMessagesRemoval -> "UserChatMessagesRemoval"
            is DeletionOfEveryMessage -> "DeletionOfEveryMessage"
            else -> throw Error(
                """
                $obj wasn't a CreatedSubscription, NewMessage, UpdatedMessage, DeletedMessage, MessageDeletionPoint, 
                UserChatMessagesRemoval, or DeletionOfEveryMessage.
                """.trimIndent()
            )
        }
        it.schema.getObjectType(type)
    }

private fun wirePrivateChatInfoSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is UpdatedAccount -> "UpdatedAccount"
            else -> throw Error("$obj wasn't a CreatedSubscription or UpdatedAccount.")
        }
        it.schema.getObjectType(type)
    }

private fun wireGroupChatInfoSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is UpdatedGroupChat -> "UpdatedGroupChat"
            is UpdatedAccount -> "UpdatedAccount"
            is ExitedUser -> "ExitedUser"
            else -> throw Error("$obj wasn't a CreatedSubscription, UpdatedGroupChat, UpdatedAccount, or ExitedUser.")
        }
        it.schema.getObjectType(type)
    }

private fun wireContactsSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder =
    builder.typeResolver {
        val type = when (val obj = it.getObject<Any>()) {
            is CreatedSubscription -> "CreatedSubscription"
            is NewContact -> "NewContact"
            is UpdatedContact -> "UpdatedContact"
            is DeletedContact -> "DeletedContact"
            else -> throw Error("$obj wasn't a CreatedSubscription, NewContact, UpdatedContact, or DeletedContact.")
        }
        it.schema.getObjectType(type)
    }

private fun wireAccountData(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = when (val obj = it.getObject<Any>()) {
        is Account -> "Account"
        is UpdatedContact -> "UpdatedContact"
        is DeletedContact -> "DeletedContact"
        is NewContact -> "NewContact"
        else -> throw Error("$obj wasn't an Account, UpdatedContact, DeletedContact, or NewContact.")
    }
    it.schema.getObjectType(type)
}

private fun wireMessageData(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder.typeResolver {
    val type = when (val obj = it.getObject<Any>()) {
        is Message -> "Message"
        is NewMessage -> "NewMessage"
        is UpdatedMessage -> "UpdatedMessage"
        else -> throw Error("$obj wasn't a Message, NewMessage, or UpdatedMessage.")
    }
    it.schema.getObjectType(type)
}