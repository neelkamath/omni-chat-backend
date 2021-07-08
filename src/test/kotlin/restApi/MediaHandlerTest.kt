package com.neelkamath.omniChatBackend.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.Audio
import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.ktor.application.*

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

/** Creates a [file] which doesn't get saved to the filesystem. An example [name] is `"pic.png"`. */
data class DummyFile(val name: String, val bytes: Int) {
    val file = ByteArray(bytes)
}

fun getFileMessage(
    path: String,
    messageId: Int,
    picType: PicType? = null,
    accessToken: String? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("message-id" to messageId.toString(), "pic-type" to picType?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    handleRequest(HttpMethod.Get, "$path?$parameters") {
        if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
    }.response
}

fun uploadMultipart(
    method: HttpMethod,
    path: String,
    parts: List<PartData>,
    parameters: String? = null,
    accessToken: String? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(method, if (parameters == null) path else "$path?$parameters") {
        if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
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
    filename: String,
    fileContent: ByteArray,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
    accessToken: String? = null,
): TestApplicationResponse =
    uploadMultipart(method, path, listOf(buildFileItem(filename, fileContent)), parameters, accessToken)

fun uploadFile(
    dummy: DummyFile,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
    accessToken: String? = null,
): TestApplicationResponse = uploadFile(dummy.name, dummy.file, method, path, parameters, accessToken)

@ExtendWith(DbExtension::class)
class MediaHandlerTest {
    @Nested
    inner class GetMediaMessage {
        @Test
        fun `A message must be read with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val audio = Audio(ByteArray(1))
            val messageId = Messages.message(admin.userId, chatId, audio)
            val response = getAudioMessage(messageId, admin.accessToken)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(audio.bytes, response.byteContent)
        }

        @Test
        fun `An HTTP status code of 401 must be returned when retrieving a non-existing message`() {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.Unauthorized, getAudioMessage(messageId = 1, token).status())
        }

        @Test
        fun `The message must be read from a public chat sans access token`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId), publicity = GroupChatPublicity.PUBLIC)
            val audio = Audio(ByteArray(1))
            val messageId = Messages.message(admin.userId, chatId, audio)
            val response = getAudioMessage(messageId = messageId)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(audio.bytes, response.byteContent)
        }

        @Test
        fun `An access token must be required to read a message which isn't from a public chat`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val audio = Audio(ByteArray(1))
            val messageId = Messages.message(admin.userId, chatId, audio)
            val response = getAudioMessage(messageId = messageId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class PostMediaMessage {
        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId), listOf(user.userId), isBroadcast = true)
            val userStatus = postAudioMessage(user.accessToken, chatId, DummyFile("audio.mp3", 1)).status()
            assertEquals(HttpStatusCode.BadRequest, userStatus)
            assertEquals(0, Messages.count())
            val adminStatus = postAudioMessage(admin.accessToken, chatId, DummyFile("audio.mp3", 1)).status()
            assertEquals(HttpStatusCode.NoContent, adminStatus)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `The message must get created sans context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val actual = postAudioMessage(admin.accessToken, chatId, DummyFile("audio.mp3", 1)).status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val contextMessageId = Messages.message(admin.userId, chatId)
            val actual =
                postAudioMessage(admin.accessToken, chatId, DummyFile("audio.mp3", 1), contextMessageId).status()
            assertEquals(HttpStatusCode.NoContent, actual)
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId))
            val response = postAudioMessage(user.accessToken, chatId, DummyFile("audio.mp3", 1))
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val result = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!).reason
            assertEquals(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT, result)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val admin = createVerifiedUsers(1).first()
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(admin.userId)) }
            val contextMessageId = Messages.message(admin.userId, chat1Id)
            val response = postAudioMessage(admin.accessToken, chat2Id, DummyFile("audio.mp3", 1), contextMessageId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val result = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!).reason
            assertEquals(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE, result)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1, user2) = createVerifiedUsers(2)
            val chatId = PrivateChats.create(user1.userId, user2.userId)
            val contextMessageId = Messages.message(user1.userId, chatId)
            PrivateChatDeletions.create(chatId, user1.userId)
            val response = postAudioMessage(user1.accessToken, chatId, DummyFile("audio.mp3", 1), contextMessageId)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val result = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!).reason
            assertEquals(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE, result)
            assertEquals(1, Messages.count())
        }

        private fun assertInvalidFile(file: DummyFile) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val response = postAudioMessage(admin.accessToken, chatId, file)
            assertEquals(HttpStatusCode.BadRequest, response.status())
            val result = testingObjectMapper.readValue<InvalidMediaMessage>(response.content!!).reason
            assertEquals(InvalidMediaMessage.Reason.INVALID_FILE, result)
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Attempting to create a message with an invalid file type must fail`(): Unit =
            assertInvalidFile(DummyFile("audio.ogg", 1))

        @Test
        fun `Attempting to create a message with an invalid file size must fail`(): Unit =
            assertInvalidFile(DummyFile("audio.mp3", Audio.MAX_BYTES + 1))
    }
}
