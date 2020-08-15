package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.tables.Users
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
import kotlin.test.assertTrue

private fun getProfilePic(userId: Int): TestApplicationResponse = withTestApplication(Application::test) {
    val parameters = listOf("user-id" to userId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "profile-pic?$parameters").response
}

private fun patchProfilePic(accessToken: String, dummy: DummyFile): TestApplicationResponse =
    uploadFile(accessToken, dummy, HttpMethod.Patch, "profile-pic")

@ExtendWith(DbExtension::class)
class ProfilePicTest {
    @Nested
    inner class GetProfilePic {
        @Test
        fun `Requesting an existing pic should cause the pic to be received with an HTTP status code of 200`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            Users.updatePic(userId, pic)
            with(getProfilePic(userId)) {
                assertEquals(HttpStatusCode.OK, status())
                assertTrue(pic.bytes.contentEquals(byteContent!!))
            }
        }

        @Test
        fun `Requesting a nonexistent pic should cause an HTTP status code of 204 to be received`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(HttpStatusCode.NoContent, getProfilePic(userId).status())
        }

        @Test
        fun `Requesting the pic of a nonexistent user should cause an HTTP status code of 400 to be received`() {
            assertEquals(HttpStatusCode.BadRequest, getProfilePic(userId = 1).status())
        }
    }

    @Nested
    inner class PatchProfilePic {
        @Test
        fun `Updating the pic should update the DB, and respond with an HTTP status code of 204`() {
            val user = createVerifiedUsers(1)[0]
            val dummy = DummyFile("pic.png", bytes = 1)
            val pic = Pic(dummy.file, Pic.Type.PNG)
            assertEquals(HttpStatusCode.NoContent, patchProfilePic(user.accessToken, dummy).status())
            assertEquals(pic, Users.read(user.info.id).pic)
        }

        private fun testBadRequest(dummy: DummyFile) {
            val token = createVerifiedUsers(1)[0].accessToken
            assertEquals(HttpStatusCode.BadRequest, patchProfilePic(token, dummy).status())
        }

        @Test
        fun `Uploading an invalid file type should fail`() {
            testBadRequest(DummyFile("pic.webp", bytes = 1))
        }

        @Test
        fun `Uploading an excessively large file should fail`() {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }
    }
}