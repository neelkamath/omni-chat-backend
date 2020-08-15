package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.test
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun patchGroupChatPic(accessToken: String, dummy: DummyFile, chatId: Int): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Patch, "group-chat-pic", parameters)
}

private fun getGroupChatPic(chatId: Int): TestApplicationResponse = withTestApplication(Application::test) {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "group-chat-pic?$parameters").response
}

@ExtendWith(DbExtension::class)
class GroupChatPicTest {
    @Nested
    inner class PatchGroupChatPic {
        @Test
        fun `Updating the pic should cause an HTTP status code of 204 to be received, and the DB to be updated`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("pic.png", bytes = 1)
            assertEquals(HttpStatusCode.NoContent, patchGroupChatPic(admin.accessToken, dummy, chatId).status())
            assertEquals(Pic(dummy.file, Pic.Type.PNG), GroupChats.readPic(chatId))
        }

        private fun testBadRequest(dummy: DummyFile) {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            assertEquals(HttpStatusCode.BadRequest, patchGroupChatPic(admin.accessToken, dummy, chatId).status())
        }

        @Test
        fun `Uploading an invalid file type should fail`() {
            testBadRequest(DummyFile("pic.webp", bytes = 1))
        }

        @Test
        fun `Uploading an excessively large file should fail`() {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }

        @Test
        fun `An HTTP status code of 401 should be received when a non-admin updates the pic`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = patchGroupChatPic(user.accessToken, DummyFile("pic.png", bytes = 1), chatId)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
            assertNull(GroupChats.readPic(chatId))
        }
    }

    @Nested
    inner class GetGroupChatPic {
        @Test
        fun `A pic should be retrieved with an HTTP status code of 200`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            GroupChats.updatePic(chatId, pic)
            with(getGroupChatPic(chatId)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(pic.bytes.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `An HTTP status code of 204 should be received when reading a nonexistent pic`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(HttpStatusCode.NoContent, getGroupChatPic(chatId).status())
        }

        @Test
        fun `An HTTP status code of 400 should be received if the chat doesn't exist`() {
            assertEquals(HttpStatusCode.BadRequest, getGroupChatPic(chatId = 1).status())
        }
    }
}