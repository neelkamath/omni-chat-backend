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

private fun patchGroupChatPic(accessToken: String, chatId: Int, fileName: String): TestApplicationResponse =
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

private fun getGroupChatPic(chatId: Int): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "group-chat-pic?$parameters").response
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
            Users.updatePic(userId, readPic("31kB.png"))
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
            val fileName = "31kB.png"
            patchProfilePic(user.accessToken, fileName).status() shouldBe HttpStatusCode.NoContent
            Users.read(user.info.id).pic shouldBe readPic(fileName)
        }
    }

    context("patchGroupChatPic(Route)") {
        test("Updating the pic should cause an HTTP status code of 204 to be received, and the DB to be updated") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val fileName = "31kB.png"
            patchGroupChatPic(admin.accessToken, chatId, fileName).status() shouldBe HttpStatusCode.NoContent
            GroupChats.readPic(chatId) shouldBe readPic(fileName)
        }

        test("Updating a nonexistent chat should cause an error to be returned") {
            val token = createVerifiedUsers(1)[0].accessToken
            with(patchGroupChatPic(token, chatId = 1, fileName = "31kB.png")) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidGroupChatPic>(content!!) shouldBe
                        InvalidGroupChatPic(InvalidGroupChatPicReason.NONEXISTENT_CHAT)
            }
        }

        test("An HTTP status code of 401 should be received when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            patchGroupChatPic(user.accessToken, chatId, "31kB.png").status() shouldBe HttpStatusCode.Unauthorized
            GroupChats.readPic(chatId).shouldBeNull()
        }
    }

    context("getGroupChatPic(Route)") {
        test("A pic should be retrieved with an HTTP status code of 200") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = readPic("31kB.png")
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

    context("PipelineContext<Unit, ApplicationCall>.readPic()") {
        fun testBadRequest(fileName: String) {
            val token = createVerifiedUsers(1)[0].accessToken
            patchProfilePic(token, fileName).status() shouldBe HttpStatusCode.BadRequest
        }

        test("Uploading an excessively large image should cause an HTTP status code of 400 to be received") {
            testBadRequest("734kB.jpg")
        }

        test("Uploading an unsupported file type should cause an HTTP status code of 400 to be received") {
            testBadRequest("39kB.webp")
        }
    }
})