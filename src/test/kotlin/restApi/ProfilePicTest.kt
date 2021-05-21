package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.db.tables.Users
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun getProfilePic(userId: Int, type: PicType): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("user-id" to userId.toString(), "pic-type" to type.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "profile-pic?$parameters").response
    }

private fun patchProfilePic(accessToken: String, filename: String): TestApplicationResponse =
    uploadFile(accessToken, filename, readBytes(filename), HttpMethod.Patch, "profile-pic")

@ExtendWith(DbExtension::class)
class ProfilePicTest {
    @Nested
    inner class GetProfileImage {
        @Test
        fun `Requesting an existing pic must cause the pic to be received with an HTTP status code of 200`() {
            val userId = createVerifiedUsers(1).first().userId
            val pic = readPic("76px×57px.jpg")
            Users.updatePic(userId, pic)
            val response = getProfilePic(userId, PicType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertTrue(pic.original.contentEquals(response.byteContent!!))
        }

        @Test
        fun `Requesting a non-existing pic must cause an HTTP status code of 204 to be received`() {
            val userId = createVerifiedUsers(1).first().userId
            assertEquals(HttpStatusCode.NoContent, getProfilePic(userId, PicType.ORIGINAL).status())
        }

        @Test
        fun `Requesting the pic of a non-existing user must cause an HTTP status code of 400 to be received`(): Unit =
            assertEquals(HttpStatusCode.BadRequest, getProfilePic(userId = 1, PicType.ORIGINAL).status())

        @Test
        fun `Requesting the original image must return the original`() {
            val userId = createVerifiedUsers(1).first().userId
            Users.updatePic(userId, readPic("1008px×756px.jpg"))
            val response = getProfilePic(userId, PicType.ORIGINAL).byteContent
            assertTrue(Users.readPic(userId, PicType.ORIGINAL).contentEquals(response))
        }

        @Test
        fun `Requesting the thumbnail must return the thumbnail`() {
            val userId = createVerifiedUsers(1).first().userId
            Users.updatePic(userId, readPic("1008px×756px.jpg"))
            val response = getProfilePic(userId, PicType.THUMBNAIL).byteContent!!
            assertTrue(Users.readPic(userId, PicType.THUMBNAIL).contentEquals(response))
        }
    }

    @Nested
    inner class PatchProfilePic {
        @Test
        fun `Updating the pic must update the DB, and respond with an HTTP status code of 204`() {
            val user = createVerifiedUsers(1).first()
            val filename = "76px×57px.jpg"
            assertEquals(HttpStatusCode.NoContent, patchProfilePic(user.accessToken, filename).status())
            val actual = Users.readPic(user.userId, PicType.ORIGINAL)
            assertTrue(readPic(filename).original.contentEquals(actual))
        }

        private fun testBadRequest(filename: String) {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.BadRequest, patchProfilePic(token, filename).status())
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("5.6MB.jpg")
    }
}
