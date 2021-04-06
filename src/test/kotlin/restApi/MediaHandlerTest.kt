package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Audio
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.GroupChatPublicity
import io.ktor.application.*

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Creates a [file] which doesn't get saved to the filesystem. An example [name] is `"pic.png"`. */
data class DummyFile(val name: String, val bytes: Int) {
    val file = ByteArray(bytes)
}

fun getFileMessage(
    accessToken: String? = null,
    path: String,
    messageId: Int,
    picType: PicType? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("message-id" to messageId.toString(), "pic-type" to picType?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    handleRequest(HttpMethod.Get, "$path?$parameters") {
        if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
    }.response
}

fun uploadMultipart(
    accessToken: String,
    method: HttpMethod,
    path: String,
    parts: List<PartData>,
    parameters: String? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(method, if (parameters == null) path else "$path?$parameters") {
        addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        val boundary = "boundary"
        addHeader(
            HttpHeaders.ContentType,
            ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString(),
        )
        setBody(boundary, parts)
    }.response
}

fun buildFileItem(filename: String, fileContent: ByteArray): PartData.FileItem = PartData.FileItem(
    { fileContent.inputStream().asInput() },
    {},
    headersOf(
        HttpHeaders.ContentDisposition,
        ContentDisposition.File
            .withParameter(ContentDisposition.Parameters.Name, "file")
            .withParameter(ContentDisposition.Parameters.FileName, filename)
            .toString(),
    ),
)

fun buildFormItem(name: String, value: String): PartData.FormItem = PartData.FormItem(
    value,
    {},
    headersOf(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, name).toString(),
    ),
)

fun uploadFile(
    accessToken: String,
    filename: String,
    fileContent: ByteArray,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
): TestApplicationResponse =
    uploadMultipart(accessToken, method, path, listOf(buildFileItem(filename, fileContent)), parameters)

fun uploadFile(
    accessToken: String,
    dummy: DummyFile,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
): TestApplicationResponse = uploadFile(accessToken, dummy.name, dummy.file, method, path, parameters)

@ExtendWith(DbExtension::class)
class MediaHandlerTest {
    @Nested
    inner class GetMediaMessage {
        @Test
        fun `A message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val audio = Audio(ByteArray(1), Audio.Type.MP3)
            val messageId = Messages.message(admin.info.id, chatId, audio)
            val response = getAudioMessage(admin.accessToken, messageId)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(audio.bytes.contentEquals(response.byteContent))
        }

        @Test
        fun `An HTTP status code of 401 must be returned when retrieving a nonexistent message`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.Unauthorized, getAudioMessage(token, messageId = 1).status())
        }

        @Test
        fun `The message must be read from a public chat sans access token`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id), publicity = GroupChatPublicity.PUBLIC)
            val audio = Audio(ByteArray(1), Audio.Type.MP3)
            val messageId = Messages.message(admin.info.id, chatId, audio)
            val response = getAudioMessage(messageId = messageId)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(audio.bytes.contentEquals(response.byteContent))
        }

        @Test
        fun `An access token must be required to read a message which isn't from a public chat`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val audio = Audio(ByteArray(1), Audio.Type.MP3)
            val messageId = Messages.message(admin.info.id, chatId, audio)
            val response = getAudioMessage(messageId = messageId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class PostMediaMessage {
        @Test
        fun `An HTTP status code of 204 must be returned when a message has been created with a context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val messageId = Messages.message(admin.info.id, chatId)
            val dummy = DummyFile("audio.mp3", bytes = 1)
            val response = postAudioMessage(admin.accessToken, dummy, chatId, contextMessageId = messageId).status()
            assertEquals(HttpStatusCode.NoContent, response)
            assertEquals(messageId, Messages.readGroupChat(chatId, userId = admin.info.id).last().node.context.id)
            assertEquals(1, AudioMessages.count())
        }

        @Test
        fun `Messaging in a nonexistent chat must fail`() {
            val token = createVerifiedUsers(1).first().accessToken
            val dummy = DummyFile("audio.mp3", bytes = 1)
            val response = postAudioMessage(token, dummy, chatId = 1)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!)
            assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT), body)
        }

        @Test
        fun `Using an invalid message context must fail`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("audio.mp3", bytes = 1)
            val response = postAudioMessage(admin.accessToken, dummy, chatId, contextMessageId = 1)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!)
            assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE), body)
        }

        private fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(listOf(admin.info.id))
            val response = postAudioMessage(admin.accessToken, dummy, chatId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val body = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!)
            assertEquals(InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE), body)
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest(DummyFile("audio.flac", bytes = 1))


        @Test
        fun `Uploading an excessively large audio file must fail`(): Unit =
            testBadRequest(DummyFile("audio.mp3", Audio.MAX_BYTES + 1))

        @Test
        fun `An HTTP status code of 401 must be returned when a non-admin creates a message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = postAudioMessage(user.accessToken, DummyFile("audio.mp3", bytes = 1), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }
}
