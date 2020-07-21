package com.neelkamath.omniChat.routing

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.create
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

class RestApiTest : FunSpec({
    context("getHealthCheck(Routing)") {
        test("A health check should respond with an HTTP status code of 204") {
            withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }
                .response
                .status() shouldBe HttpStatusCode.NoContent
        }
    }

    context("getProfilePic(Route)") {
        fun getProfilePic(userId: Int): TestApplicationResponse = withTestApplication(Application::main) {
            val parameters = listOf("user-id" to userId.toString()).formUrlEncode()
            handleRequest(HttpMethod.Get, "profile-pic?$parameters").response
        }

        test("Requesting an existing pic should cause the pic to be received with an HTTP status code of 200") {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, readImage("31kB.png"))
            with(getProfilePic(userId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe Users.read(userId).pic!!
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
        fun patchProfilePic(accessToken: String, fileName: String): TestApplicationResponse =
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
                                { readInput(fileName) },
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

        test("Updating the pic should update the DB, and send an HTTP status code of 400") {
            val user = createVerifiedUsers(1)[0]
            val fileName = "31kB.png"
            patchProfilePic(user.accessToken, fileName).status() shouldBe HttpStatusCode.NoContent
            Users.read(user.info.id).pic shouldBe readImage(fileName)
        }

        test(
            """
                Uploading a large pic shouldn't cause the profile pic to be updated, and should cause an HTTP status 
                code of 400 to be received
                """
        ) {
            val user = createVerifiedUsers(1)[0]
            patchProfilePic(user.accessToken, "734kB.jpg").status() shouldBe HttpStatusCode.BadRequest
            Users.read(user.info.id).pic.shouldBeNull()
        }
    }

    context("patchGroupChatPic(Route)") {
        fun patchGroupChatPic(accessToken: String, chatId: Int, fileName: String): TestApplicationResponse =
            withTestApplication(Application::main) {
                val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
                handleRequest(HttpMethod.Patch, "group-chat-pic?$parameters") {
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
                                { readInput(fileName) },
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

        test("Updating the pic should cause an HTTP status code of 204 to be received, and the DB to be updated") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(admin.info.id)
            val fileName = "31kB.png"
            patchGroupChatPic(admin.accessToken, chatId, fileName).status() shouldBe HttpStatusCode.NoContent
            GroupChats.readPic(chatId) shouldBe readImage(fileName)
        }

        fun testBadRequest(response: TestApplicationResponse, reason: InvalidGroupChatPicReason): Unit =
            with(response) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidGroupChatPic>(content!!) shouldBe InvalidGroupChatPic(reason)
            }

        test("Updating a nonexistent chat should cause an error to be returned") {
            val token = createVerifiedUsers(1)[0].accessToken
            testBadRequest(
                patchGroupChatPic(token, chatId = 1, fileName = "31kB.png"),
                InvalidGroupChatPicReason.NONEXISTENT_CHAT
            )
        }

        test("Updating a chat with an excessively large image should cause an error to be returned") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(admin.info.id)
            val fileName = "734kB.jpg"
            testBadRequest(
                patchGroupChatPic(admin.accessToken, chatId, fileName),
                InvalidGroupChatPicReason.PIC_TOO_BIG
            )
            GroupChats.readPic(chatId).shouldBeNull()
        }

        test("An HTTP status code of 401 should be received when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
            patchGroupChatPic(user.accessToken, chatId, "31kB.png").status() shouldBe HttpStatusCode.Unauthorized
            GroupChats.readPic(chatId).shouldBeNull()
        }
    }

    context("getGroupChatPic(Route)") {
        fun getGroupChatPic(chatId: Int): TestApplicationResponse = withTestApplication(Application::main) {
            val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
            handleRequest(HttpMethod.Get, "group-chat-pic?$parameters").response
        }

        test("A pic should be retrieved with an HTTP status code of 200") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val image = readImage("31kB.png")
            GroupChats.updatePic(chatId, image)
            with(getGroupChatPic(chatId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe image
            }
        }

        test("An HTTP status code of 204 should be received when reading a nonexistent pic") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            getGroupChatPic(chatId).status() shouldBe HttpStatusCode.NoContent
        }

        test("An HTTP status code of 400 should be received if the chat doesn't exist") {
            getGroupChatPic(chatId = 1).status() shouldBe HttpStatusCode.BadRequest
        }
    }
})