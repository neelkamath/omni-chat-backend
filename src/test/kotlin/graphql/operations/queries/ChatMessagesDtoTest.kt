package com.neelkamath.omniChat.graphql.operations.queries

import com.neelkamath.omniChat.TextMessage
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChatMessagesDtoTest : FunSpec({
    context("getMessages(DataFetchingEnvironment") {
        /** Data on a group chat having only ever contained an admin. */
        data class AdminMessages(
            /** The access token of the chat's admin. */
            val adminToken: String,
            /** Every message sent has this text. */
            val text: TextMessage,
            /** The ten messages the admin sent. */
            val messageIdList: List<Int>
        )

        fun createUtilizedChat(): AdminMessages {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(admin.info.id, buildNewGroupChat())
            val message = TextMessage("t")
            val messageIdList = (1..10).map { Messages.message(chatId, admin.info.id, message) }
            return AdminMessages(admin.accessToken, message, messageIdList)
        }

        fun testPagination(shouldDeleteMessage: Boolean) {
            val (adminToken, queryText, messageIdList) = createUtilizedChat()
            val index = 5
            if (shouldDeleteMessage) Messages.delete(messageIdList[index])
            val last = 3
            searchMessages(
                adminToken,
                queryText.value,
                chatMessagesPagination = BackwardPagination(last, before = messageIdList[index])
            ).flatMap { it.messages }.map { it.cursor } shouldBe messageIdList.take(index).takeLast(last)
        }

        test("Messages should paginate using a cursor from a deleted message as if the message still exists") {
            testPagination(shouldDeleteMessage = true)
        }

        test("If neither cursor nor limit are supplied, every message should be retrieved") {
            val (adminToken, queryText, messageIdList) = createUtilizedChat()
            searchMessages(adminToken, queryText.value).flatMap { it.messages }.map { it.cursor } shouldBe messageIdList
        }

        test("Only the messages specified by the cursor and limit should be retrieved") {
            testPagination(shouldDeleteMessage = false)
        }
    }
})