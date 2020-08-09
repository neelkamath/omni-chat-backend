package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.GroupChatDescription
import com.neelkamath.omniChat.graphql.routing.GroupChatInput
import com.neelkamath.omniChat.graphql.routing.GroupChatTitle
import com.neelkamath.omniChat.testingObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse

private fun getPicMessage(accessToken: String, messageId: Int): TestApplicationResponse =
    getFileMessage(accessToken, messageId, path = "pic-message")

private fun postPicMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    caption: String? = null,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString(),
        "caption" to caption
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "pic-message", parameters)
}

class PicMessageTest : FunSpec({
    context("getPicMessage(Route)") {
        test("A pic message should be read with an HTTP status code of 200") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val messageId = Messages.message(admin.info.id, chatId, CaptionedPic(pic, caption = null))
            with(getPicMessage(admin.accessToken, messageId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe pic.bytes
            }
        }

        test("An HTTP status code of 400 should be returned when retrieving a nonexistent message") {
            val token = createVerifiedUsers(1)[0].accessToken
            getPicMessage(token, messageId = 1).status() shouldBe HttpStatusCode.BadRequest
        }
    }

    context("postPicMessage(Route)") {
        test("An HTTP status code of 204 should be returned when a captioned pic with a context has been created") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val dummy = DummyFile("pic.png", bytes = 1)
            postPicMessage(admin.accessToken, dummy, chatId, "caption", contextMessageId = messageId).status() shouldBe
                    HttpStatusCode.NoContent
            Messages.readGroupChat(admin.info.id, chatId).last().node.context.id shouldBe messageId
            PicMessages.count() shouldBe 1
        }

        test("Messaging in a nonexistent chat should fail") {
            val token = createVerifiedUsers(1)[0].accessToken
            val dummy = DummyFile("pic.png", bytes = 1)
            with(postPicMessage(token, dummy, chatId = 1)) {
                status() shouldBe HttpStatusCode.BadRequest
                testingObjectMapper.readValue<InvalidMediaMessage>(content!!) shouldBe
                        InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT)
            }
        }

        test("Using an invalid message context should fail") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            with(postPicMessage(admin.accessToken, dummy, chatId, contextMessageId = 1)) {
                status() shouldBe HttpStatusCode.BadRequest
                testingObjectMapper.readValue<InvalidMediaMessage>(content!!) shouldBe
                        InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE)
            }
        }

        fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postPicMessage(admin.accessToken, dummy, chatId)) {
                status() shouldBe HttpStatusCode.BadRequest
                testingObjectMapper.readValue<InvalidMediaMessage>(content!!) shouldBe
                        InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE)
            }
        }

        test("Uploading an invalid file type should fail") { testBadRequest(DummyFile("pic.webp", bytes = 1)) }

        test("Uploading an excessively large audio file should fail") {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }

        test("Using an invalid caption should fail") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            postPicMessage(admin.accessToken, dummy, chatId, caption = "")
        }

        test("An HTTP status code of 401 should be returned when a non-admin creates a message in a broadcast chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(admin.info.id, user.info.id),
                adminIdList = listOf(admin.info.id),
                isBroadcast = true
            )
            val chatId = GroupChats.create(chat)
            postPicMessage(user.accessToken, DummyFile("pic.png", bytes = 1), chatId).status() shouldBe
                    HttpStatusCode.Unauthorized
        }
    }
})