package com.neelkamath.omniChat.restApi

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

private fun patchGroupChatPic(accessToken: String, dummy: DummyFile, chatId: Int): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Patch, "group-chat-pic", parameters)
}

private fun getGroupChatPic(chatId: Int): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "group-chat-pic?$parameters").response
}

class GroupChatPicTest : FunSpec({
    context("patchGroupChatPic(Route)") {
        test("Updating the pic should cause an HTTP status code of 204 to be received, and the DB to be updated") {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            patchGroupChatPic(admin.accessToken, dummy, chatId).status() shouldBe HttpStatusCode.NoContent
            GroupChats.readPic(chatId) shouldBe Pic(dummy.file, Pic.Type.PNG)
        }

        test("Updating a nonexistent chat should cause an error to be returned") {
            val token = createVerifiedUsers(1)[0].accessToken
            val dummy = DummyFile("pic.png", bytes = 1)
            with(patchGroupChatPic(token, dummy, chatId = 1)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidGroupChatPicUpdate>(content!!) shouldBe
                        InvalidGroupChatPicUpdate(InvalidGroupChatPicUpdate.Reason.USER_NOT_IN_CHAT)
            }
        }

        test("Using a private chat should fail") {
            val (user1, user2) = createVerifiedUsers(2)
            val chatId = PrivateChats.create(user1.info.id, user2.info.id)
            val dummy = DummyFile("pic.png", bytes = 1)
            with(patchGroupChatPic(user1.accessToken, dummy, chatId)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidGroupChatPicUpdate>(content!!) shouldBe
                        InvalidGroupChatPicUpdate(InvalidGroupChatPicUpdate.Reason.USER_NOT_IN_CHAT)
            }
        }

        fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            with(patchGroupChatPic(admin.accessToken, dummy, chatId)) {
                status() shouldBe HttpStatusCode.BadRequest
                objectMapper.readValue<InvalidGroupChatPicUpdate>(content!!) shouldBe
                        InvalidGroupChatPicUpdate(InvalidGroupChatPicUpdate.Reason.INVALID_FILE)
            }
        }

        test("Uploading an invalid file type should fail") { testBadRequest(DummyFile("pic.webp", bytes = 1)) }

        test("Uploading an excessively large file should fail") {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }

        test("An HTTP status code of 401 should be received when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            patchGroupChatPic(user.accessToken, DummyFile("pic.png", bytes = 1), chatId).status() shouldBe
                    HttpStatusCode.Unauthorized
            GroupChats.readPic(chatId).shouldBeNull()
        }
    }

    context("getGroupChatPic(Route)") {
        test("A pic should be retrieved with an HTTP status code of 200") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
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
})