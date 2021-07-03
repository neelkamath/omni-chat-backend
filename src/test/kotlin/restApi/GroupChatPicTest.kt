package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

private fun getGroupChatPic(accessToken: String? = null, chatId: Int, type: PicType): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("chat-id" to chatId.toString(), "pic-type" to type.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "group-chat-pic?$parameters") {
            if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        }.response
    }

private fun patchGroupChatPic(accessToken: String, chatId: Int, filename: String): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString()).formUrlEncode()
    return uploadFile(accessToken, filename, readBytes(filename), HttpMethod.Patch, "group-chat-pic", parameters)
}

@ExtendWith(DbExtension::class)
class GroupChatPicTest {
    @Nested
    inner class PatchGroupChatPic {
        @Test
        fun `Updating the pic must cause an HTTP status code of 204 to be received, and the DB to be updated`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val filename = "76px×57px.jpg"
            val pic = readPic(filename)
            assertEquals(HttpStatusCode.NoContent, patchGroupChatPic(admin.accessToken, chatId, filename).status())
            val original = GroupChats.readPic(chatId, PicType.ORIGINAL)
            assertTrue(pic.original.contentEquals(original))
            val thumbnail = GroupChats.readPic(chatId, PicType.THUMBNAIL)
            assertTrue(pic.thumbnail.contentEquals(thumbnail))
        }

        private fun testBadRequest(filename: String) {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val actual = patchGroupChatPic(admin.accessToken, chatId, filename).status()
            assertEquals(HttpStatusCode.BadRequest, actual)
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("3.9MB.jpg")

        @Test
        fun `An HTTP status code of 401 must be received when a non-admin updates the pic`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId), listOf(user.userId))
            val actual = patchGroupChatPic(user.accessToken, chatId, "76px×57px.jpg").status()
            assertEquals(HttpStatusCode.Unauthorized, actual)
            assertNull(GroupChats.readPic(chatId, PicType.ORIGINAL))
        }
    }

    private data class ChatPic(val chatId: Int, val pic: Pic)

    @Nested
    inner class GetGroupChatPic {
        @Test
        fun `A pic must be retrieved with an HTTP status code of 200`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val pic = readPic("76px×57px.jpg")
            GroupChats.updatePic(chatId, pic)
            val response = getGroupChatPic(admin.accessToken, chatId, PicType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(pic.original, response.byteContent)
        }

        @Test
        fun `An HTTP status code of 204 must be received when reading a non-existing pic`() {
            val admin = createVerifiedUsers(1).first()
            val chatId = GroupChats.create(setOf(admin.userId))
            val status = getGroupChatPic(admin.accessToken, chatId, PicType.ORIGINAL).status()
            assertEquals(HttpStatusCode.NoContent, status)
        }

        @Test
        fun `An HTTP status code of 400 must be received if the chat doesn't exist`(): Unit =
            assertEquals(HttpStatusCode.BadRequest, getGroupChatPic(chatId = 1, type = PicType.ORIGINAL).status())

        private fun createGroupChat(): ChatPic {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val pic = readPic("1008px×756px.jpg")
            GroupChats.updatePic(chatId, pic)
            return ChatPic(chatId, pic)
        }

        @Test
        fun `The original pic must be received when requested`() {
            val (chatId, pic) = createGroupChat()
            val response = getGroupChatPic(chatId = chatId, type = PicType.ORIGINAL).byteContent
            assertContentEquals(pic.original, response)
        }

        @Test
        fun `The thumbnail must be received when requested`() {
            val (chatId, pic) = createGroupChat()
            val response = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).byteContent
            assertContentEquals(pic.thumbnail, response)
        }

        @Test
        fun `The pic must be retrieved from a public chat sans access token`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val status = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.NoContent, status)
        }

        @Test
        fun `The pic must not be retrieved from a chat sans access token`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val status = getGroupChatPic(chatId = chatId, type = PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        @Test
        fun `The pic must not be retrieved from a chat the user isn't in`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(setOf(admin.userId))
            val status = getGroupChatPic(user.accessToken, chatId, PicType.THUMBNAIL).status()
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}
