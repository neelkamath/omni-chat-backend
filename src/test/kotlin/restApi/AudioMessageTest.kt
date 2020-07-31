package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse

private fun postAudioMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString()
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "audio-message", parameters)
}

private fun getAudioMessage(accessToken: String, messageId: Int): TestApplicationResponse =
    getFileMessage(accessToken, messageId, path = "audio-message")

class AudioMessageTest : FunSpec({
    context("getAudioMessage(Route)") {
        test("An audio message should be read with an HTTP status code of 200") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val audio = Mp3(ByteArray(1))
            val messageId = Messages.message(admin.info.id, chatId, audio)
            with(getAudioMessage(admin.accessToken, messageId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe audio.bytes
            }
        }

        test("An HTTP status code of 400 should be returned when retrieving a nonexistent message") {
            val token = createVerifiedUsers(1)[0].accessToken
            getAudioMessage(token, messageId = 1).status() shouldBe HttpStatusCode.BadRequest
        }
    }

    context("postAudioMessage(Route)") {
        test("An HTTP status code of 204 should be returned when a message has been created with a context") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val dummy = DummyFile("audio.mp3", bytes = 1)
            postAudioMessage(admin.accessToken, dummy, chatId, contextMessageId = messageId).status() shouldBe
                    HttpStatusCode.NoContent
            Messages.readGroupChat(admin.info.id, chatId).last().node.context.id shouldBe messageId
            AudioMessages.count() shouldBe 1
        }

        test("Messaging in a nonexistent chat should fail") {
            val token = createVerifiedUsers(1)[0].accessToken
            val dummy = DummyFile("audio.mp3", bytes = 1)
            with(postAudioMessage(token, dummy, chatId = 1)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.USER_NOT_IN_CHAT)
            }
        }

        fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postAudioMessage(admin.accessToken, dummy, chatId)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE)
            }
        }

        test("Uploading an invalid file type should fail") { testBadRequest(DummyFile("audio.flac", bytes = 1)) }

        test("Uploading an excessively large audio file should fail") {
            testBadRequest(DummyFile("audio.mp3", Mp3.MAX_BYTES + 1))
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
            postAudioMessage(user.accessToken, DummyFile("audio.mp3", bytes = 1), chatId).status() shouldBe
                    HttpStatusCode.Unauthorized
        }
    }
})