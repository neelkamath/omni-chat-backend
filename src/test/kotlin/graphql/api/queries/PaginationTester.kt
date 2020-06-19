package com.neelkamath.omniChat.graphql.api.queries

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.graphql.api.messageAndReadId
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.matchers.shouldBe

enum class OperationName {
    /** Represents `Query.searchChatMessages`. */
    SEARCH_CHAT_MESSAGES,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS
}

/** Asserts that the [operation] paginates correctly. */
fun testPagination(operation: OperationName) {
    val adminToken = createSignedInUsers(1)[0].accessToken
    val title = "Title"
    val chatId = createGroupChat(adminToken, NewGroupChat(title))
    val text = "text"
    val messageIdList = (1..10).map { messageAndReadId(adminToken, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    when (operation) {
        OperationName.SEARCH_CHAT_MESSAGES -> searchChatMessages(adminToken, chatId, text, pagination)
        OperationName.SEARCH_MESSAGES -> searchMessages(adminToken, text, pagination).flatMap { it.messages }
        OperationName.READ_CHATS -> readChats(adminToken, pagination)[0].messages.edges
        OperationName.READ_CHAT -> readChat(adminToken, chatId, pagination).messages.edges
        OperationName.SEARCH_CHATS -> searchChats(adminToken, title, pagination)[0].messages.edges
    }.map { it.cursor } shouldBe messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last)
}