package com.neelkamath.omniChat.routing

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.streams.asInput
import java.lang.ClassLoader.getSystemClassLoader

private fun getHealthCheck(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

private fun getProfilePic(userId: Int): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("user-id" to userId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "profile-pic?$parameters").response
}

private fun patchProfilePic(accessToken: String, fileName: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "profile-pic") {
            addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
            val boundary = "boundary"
            addHeader(
                HttpHeaders.ContentType,
                ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
            )
            setBody(
                boundary,
                listOf(
                    PartData.FileItem(
                        { getSystemClassLoader().getResource(fileName)!!.openStream().asInput() },
                        {},
                        headersOf(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.File
                                .withParameter(ContentDisposition.Parameters.Name, "file")
                                .withParameter(ContentDisposition.Parameters.FileName, fileName)
                                .toString()
                        )
                    )
                )
            )
        }
    }.response

private fun patchGroupChatPic(accessToken: String, chatId: Int, fileName: String): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(accessToken, parameters, fileName, HttpMethod.Patch, "group-chat-pic")
}

private fun getGroupChatPic(chatId: Int): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "group-chat-pic?$parameters").response
}

private fun postAudioMessage(
    accessToken: String,
    chatId: Int,
    fileName: String,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf(
        "chat-id" to chatId.toString(),
        "context-message-id" to contextMessageId?.toString()
    ).filter { it.second != null }.formUrlEncode()
    return uploadFile(accessToken, parameters, fileName, HttpMethod.Post, "audio-message")
}

private fun getAudioMessage(accessToken: String, messageId: Int): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("message-id" to messageId.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "audio-message?$parameters") {
            addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        }.response
    }

private fun uploadFile(
    accessToken: String,
    parameters: String,
    fileName: String,
    method: HttpMethod,
    path: String
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(method, "$path?$parameters") {
        addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        val boundary = "boundary"
        addHeader(
            HttpHeaders.ContentType,
            ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
        )
        setBody(
            boundary,
            listOf(
                PartData.FileItem(
                    { getSystemClassLoader().getResource(fileName)!!.openStream().asInput() },
                    {},
                    headersOf(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File
                            .withParameter(ContentDisposition.Parameters.Name, "file")
                            .withParameter(ContentDisposition.Parameters.FileName, fileName)
                            .toString()
                    )
                )
            )
        )
    }.response
}

class RestApiTest : FunSpec({
    context("getHealthCheck(Routing)") {
        test("A health check should respond with an HTTP status code of 204") {
            getHealthCheck().status() shouldBe HttpStatusCode.NoContent
        }
    }

    context("getProfilePic(Route)") {
        test("Requesting an existing pic should cause the pic to be received with an HTTP status code of 200") {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, Pic.build("31KB.png"))
            with(getProfilePic(userId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe Users.read(userId).pic!!.bytes
            }
        }

        test("Requesting a nonexistent pic should cause an HTTP status code of 204 to be received") {
            val userId = createVerifiedUsers(1)[0].info.id
            getProfilePic(userId).status() shouldBe HttpStatusCode.NoContent
        }

        test("Requesting the pic of a nonexistent user should cause an HTTP status code of 400 to be received") {
            getProfilePic(userId = 1).status() shouldBe HttpStatusCode.BadRequest
        }
    }

    context("patchProfilePic(Route)") {
        test("Updating the pic should update the DB, and respond with an HTTP status code of 204") {
            val user = createVerifiedUsers(1)[0]
            val fileName = "31KB.png"
            patchProfilePic(user.accessToken, fileName).status() shouldBe HttpStatusCode.NoContent
            Users.read(user.info.id).pic shouldBe Pic.build(fileName)
        }

        fun testBadRequest(fileName: String) {
            val token = createVerifiedUsers(1)[0].accessToken
            patchProfilePic(token, fileName).status() shouldBe HttpStatusCode.BadRequest
        }

        test("Uploading an invalid file type should fail") { testBadRequest("17KB.webp") }

        test("Uploading an excessively large file should fail") { testBadRequest("2MB.jpg") }
    }

    context("patchGroupChatPic(Route)") {
        test("Updating the pic should cause an HTTP status code of 204 to be received, and the DB to be updated") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val fileName = "31KB.png"
            patchGroupChatPic(admin.accessToken, chatId, fileName).status() shouldBe HttpStatusCode.NoContent
            GroupChats.readPic(chatId) shouldBe Pic.build(fileName)
        }

        test("Updating a nonexistent chat should cause an error to be returned") {
            val token = createVerifiedUsers(1)[0].accessToken
            with(patchGroupChatPic(token, chatId = 1, fileName = "31KB.png")) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_CHAT_ID)
            }
        }

        test("Using a private chat should fail") {
            val (user1, user2) = createVerifiedUsers(2)
            val chatId = PrivateChats.create(user1.info.id, user2.info.id)
            with(patchGroupChatPic(user1.accessToken, chatId, "31KB.png")) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_CHAT_ID)
            }
        }

        fun testBadRequest(fileName: String) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(patchGroupChatPic(admin.accessToken, chatId, fileName)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE)
            }
        }

        test("Uploading an invalid file type should fail") { testBadRequest("17KB.webp") }

        test("Uploading an excessively large file should fail") { testBadRequest("2MB.jpg") }

        test("An HTTP status code of 401 should be received when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            patchGroupChatPic(user.accessToken, chatId, "31KB.png").status() shouldBe HttpStatusCode.Unauthorized
            GroupChats.readPic(chatId).shouldBeNull()
        }
    }

    context("getGroupChatPic(Route)") {
        test("A pic should be retrieved with an HTTP status code of 200") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = Pic.build("31KB.png")
            GroupChats.updatePic(chatId, pic)
            with(getGroupChatPic(chatId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe pic.bytes
            }
        }

        test("An HTTP status code of 204 should be received when reading a nonexistent pic") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            getGroupChatPic(chatId).status() shouldBe HttpStatusCode.NoContent
        }

        test("An HTTP status code of 400 should be received if the chat doesn't exist") {
            getGroupChatPic(chatId = 1).status() shouldBe HttpStatusCode.BadRequest
        }
    }

    context("getAudioMessage(Route)") {
        test("An audio message should be read with an HTTP status code of 200") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val audio = buildMp3("215KB.mp3")
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
            postAudioMessage(admin.accessToken, chatId, "215KB.mp3", contextMessageId = messageId).status() shouldBe
                    HttpStatusCode.NoContent
            Messages.readGroupChat(admin.info.id, chatId).last().node.context.id shouldBe messageId
            AudioMessages.count() shouldBe 1
        }

        test("Messaging in a nonexistent chat should fail") {
            val token = createVerifiedUsers(1)[0].accessToken
            with(postAudioMessage(token, chatId = 1, fileName = "215KB.mp3")) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_CHAT_ID)
            }
        }

        fun testBadRequest(fileName: String) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(postAudioMessage(admin.accessToken, chatId, fileName)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidFileUpload>(content!!) shouldBe
                        InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE)
            }
        }

        test("Uploading an invalid file type should fail") { testBadRequest("193KB.flac") }

        test("Uploading an excessively large audio file should fail") { testBadRequest("2MB.mp3") }

        test("An HTTP status code of 401 should be returned when a non-admin creates a message in a broadcast chatƒ") {
            val (admin, user) = createVerifiedUsers(2)
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(admin.info.id, user.info.id),
                adminIdList = listOf(admin.info.id),
                isBroadcast = true
            )
            val chatId = GroupChats.create(chat)
            postAudioMessage(user.accessToken, chatId, "215KB.mp3").status() shouldBe HttpStatusCode.Unauthorized
        }
    }
})