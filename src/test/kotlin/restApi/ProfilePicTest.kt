package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

private fun getProfilePic(userId: Int): TestApplicationResponse = withTestApplication(Application::test) {
    val parameters = listOf("user-id" to userId.toString()).formUrlEncode()
    handleRequest(HttpMethod.Get, "profile-pic?$parameters").response
}

private fun patchProfilePic(accessToken: String, dummy: DummyFile): TestApplicationResponse =
    uploadFile(accessToken, dummy, HttpMethod.Patch, "profile-pic")

class ProfilePicTest : FunSpec({
    context("getProfilePic(Route)") {
        test("Requesting an existing pic should cause the pic to be received with an HTTP status code of 200") {
            val userId = createVerifiedUsers(1)[0].info.id
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            Users.updatePic(userId, pic)
            with(getProfilePic(userId)) {
                status() shouldBe HttpStatusCode.OK
                byteContent shouldBe pic.bytes
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
            val dummy = DummyFile("pic.png", bytes = 1)
            val pic = Pic(dummy.file, Pic.Type.PNG)
            patchProfilePic(user.accessToken, dummy).status() shouldBe HttpStatusCode.NoContent
            Users.read(user.info.id).pic shouldBe pic
        }

        fun testBadRequest(dummy: DummyFile) {
            val token = createVerifiedUsers(1)[0].accessToken
            patchProfilePic(token, dummy).status() shouldBe HttpStatusCode.BadRequest
        }

        test("Uploading an invalid file type should fail") { testBadRequest(DummyFile("pic.webp", bytes = 1)) }

        test("Uploading an excessively large file should fail") {
            testBadRequest(DummyFile("pic.png", Pic.MAX_BYTES + 1))
        }
    }
})