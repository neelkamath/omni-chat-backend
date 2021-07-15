package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.tables.Users
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

private fun getProfileImage(userId: Int, type: ImageType): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = listOf("user-id" to userId.toString(), "image-type" to type.toString()).formUrlEncode()
        handleRequest(HttpMethod.Get, "profile-image?$parameters").response
    }

private fun patchProfileImage(accessToken: String, filename: String): TestApplicationResponse =
    uploadFile(filename, readBytes(filename), HttpMethod.Patch, "profile-image", accessToken = accessToken)

@ExtendWith(DbExtension::class)
class ProfileImageTest {
    @Nested
    inner class GetProfileImage {
        @Test
        fun `Requesting an existing image must cause the image to be received with an HTTP status code of 200`() {
            val userId = createVerifiedUsers(1).first().userId
            val image = readImage("76px×57px.jpg")
            Users.updateImage(userId, image)
            val response = getProfileImage(userId, ImageType.ORIGINAL)
            assertEquals(HttpStatusCode.OK, response.status())
            assertContentEquals(image.original, response.byteContent!!)
        }

        @Test
        fun `Requesting a non-existing image must cause an HTTP status code of 204 to be received`() {
            val userId = createVerifiedUsers(1).first().userId
            assertEquals(HttpStatusCode.NoContent, getProfileImage(userId, ImageType.ORIGINAL).status())
        }

        @Test
        fun `Requesting the image of a non-existing user must cause an HTTP status code of 400 to be received`(): Unit =
            assertEquals(HttpStatusCode.BadRequest, getProfileImage(userId = 1, ImageType.ORIGINAL).status())

        @Test
        fun `Requesting the original image must return the original`() {
            val userId = createVerifiedUsers(1).first().userId
            Users.updateImage(userId, readImage("1008px×756px.jpg"))
            val expected = Users.readImage(userId, ImageType.ORIGINAL)!!.bytes
            val actual = getProfileImage(userId, ImageType.ORIGINAL).byteContent
            assertContentEquals(expected, actual)
        }

        @Test
        fun `Requesting the thumbnail must return the thumbnail`() {
            val userId = createVerifiedUsers(1).first().userId
            Users.updateImage(userId, readImage("1008px×756px.jpg"))
            val expected = Users.readImage(userId, ImageType.THUMBNAIL)!!.bytes
            val actual = getProfileImage(userId, ImageType.THUMBNAIL).byteContent!!
            assertContentEquals(expected, actual)
        }
    }

    @Nested
    inner class PatchProfileImage {
        @Test
        fun `Updating the image must update the DB, and respond with an HTTP status code of 204`() {
            val user = createVerifiedUsers(1).first()
            val filename = "76px×57px.jpg"
            assertEquals(HttpStatusCode.NoContent, patchProfileImage(user.accessToken, filename).status())
            val actual = Users.readImage(user.userId, ImageType.ORIGINAL)!!.bytes
            assertContentEquals(readImage(filename).original, actual)
        }

        private fun testBadRequest(filename: String) {
            val token = createVerifiedUsers(1).first().accessToken
            assertEquals(HttpStatusCode.BadRequest, patchProfileImage(token, filename).status())
        }

        @Test
        fun `Uploading an invalid file type must fail`(): Unit = testBadRequest("76px×57px.webp")

        @Test
        fun `Uploading an excessively large file must fail`(): Unit = testBadRequest("3.9MB.jpg")
    }
}
