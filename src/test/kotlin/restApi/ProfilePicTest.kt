package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.tables.Users
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

private fun patchProfilePic(accessToken: String, filename: String, fileContent: ByteArray): TestApplicationResponse =
    uploadFile(accessToken, filename, fileContent, HttpMethod.Patch, "profile-pic")

private fun patchProfilePic(accessToken: String, pic: Pic): TestApplicationResponse =
    patchProfilePic(accessToken, "img.${pic.type}", pic.original)

@ExtendWith(DbExtension::class)
class ProfilePicTest {
    @Nested
    inner class GetProfileImage {
        @Test
        fun `Requesting an existing pic must cause the pic to be received with an HTTP status code of 200`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val pic = readPic("76px×57px.jpg")
            Users.updatePic(userId, pic)
            with(getProfilePic(userId, PicType.ORIGINAL)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(pic.original.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `Requesting a nonexistent pic must cause an HTTP status code of 204 to be received`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(HttpStatusCode.NoContent, getProfilePic(userId, PicType.ORIGINAL).status())
        }

        @Test
        fun `Requesting the pic of a nonexistent user must cause an HTTP status code of 400 to be received`() {
            assertEquals(HttpStatusCode.BadRequest, getProfilePic(userId = 1, PicType.ORIGINAL).status())
        }

        @Test
        fun `Requesting the original image must return the original`() {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, readPic("1008px×756px.jpg"))
            val response = getProfilePic(userId, PicType.ORIGINAL).byteContent
            assertTrue(Users.read(userId).pic!!.original.contentEquals(response))
        }

        @Test
        fun `Requesting the thumbnail must return the thumbnail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, readPic("1008px×756px.jpg"))
            val response = getProfilePic(userId, PicType.THUMBNAIL).byteContent!!
            assertTrue(Users.read(userId).pic!!.thumbnail.contentEquals(response))
        }
    }

    @Nested
    inner class PatchProfilePic {
        @Test
        fun `Updating the pic must update the DB, and respond with an HTTP status code of 204`() {
            val user = createVerifiedUsers(1)[0]
            val pic = readPic("76px×57px.jpg")
            assertEquals(HttpStatusCode.NoContent, patchProfilePic(user.accessToken, pic).status())
            assertEquals(pic, Users.read(user.info.id).pic)
        }

        private fun testBadRequest(filename: String, fileContent: ByteArray) {
            val token = createVerifiedUsers(1)[0].accessToken
            assertEquals(HttpStatusCode.BadRequest, patchProfilePic(token, filename, fileContent).status())
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit =
            "76px×57px.webp".let { testBadRequest(it, readBytes(it)) }

        @Test
        fun `Uploading an excessively large file must fail`(): Unit =
            "5.6MB.jpg".let { testBadRequest(it, readBytes(it)) }
    }
}
