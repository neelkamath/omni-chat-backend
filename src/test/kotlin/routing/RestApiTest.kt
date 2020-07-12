package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.setProfilePic
import com.neelkamath.omniChat.main
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
import io.ktor.utils.io.streams.asInput
import java.lang.ClassLoader.getSystemClassLoader

class RestApiTest : FunSpec({
    context("getHealthCheck(Routing)") {
        test("A health check should respond with an HTTP status code of 204") {
            withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }
                .response
                .status() shouldBe HttpStatusCode.NoContent
        }
    }

    context("routeProfilePic(Routing)") {
        fun getProfilePic(accessToken: String): TestApplicationResponse = withTestApplication(Application::main) {
            handleRequest(HttpMethod.Get, "profile-pic") { addHeader(HttpHeaders.Authorization, "Bearer $accessToken") }
        }.response

        test("Requesting a nonexistent pic should cause an HTTP status code of 204 to be received") {
            val token = createVerifiedUsers(1)[0].accessToken
            getProfilePic(token).status() shouldBe HttpStatusCode.NoContent
        }

        test("Requesting an existing pic should cause the pic to be received with an HTTP status code of 200") {
            val user = createVerifiedUsers(1)[0]
            Users.setProfilePic(user.info.id)
            with(getProfilePic(user.accessToken)) {
                status() shouldBe HttpStatusCode.OK
                byteContent!! shouldBe Users.readProfilePic(user.info.id)!!
            }
        }

        fun patchProfilePic(accessToken: String, fileName: String = "31kB.png"): TestApplicationResponse =
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
                                { getSystemClassLoader().getResource(fileName)!!.openStream().asInput() },
                                { },
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
            patchProfilePic(user.accessToken).status() shouldBe HttpStatusCode.NoContent
            Users.readProfilePic(user.info.id) shouldBe getSystemClassLoader().getResource("31kB.png")!!.readBytes()
        }

        test(
            """
            Uploading a large pic shouldn't cause the profile pic to be updated, and should cause an HTTP status code of
            400 to be returned
            """
        ) {
            val user = createVerifiedUsers(1)[0]
            patchProfilePic(user.accessToken, "734kB.jpg").status() shouldBe HttpStatusCode.BadRequest
            Users.readProfilePic(user.info.id).shouldBeNull()
        }
    }
})