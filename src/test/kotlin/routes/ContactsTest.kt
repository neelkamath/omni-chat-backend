package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.test.db.read
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun createContacts(userIdList: UserIdList, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "contacts") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(userIdList))
        }
    }.response

fun readContacts(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "contacts") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun deleteContacts(userIdList: UserIdList, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Delete, "contacts") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(userIdList))
        }
    }.response

class DeleteContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be deleted ignoring invalid ones" {
        val users = createVerifiedUsers(3)
        val userIdList = setOf(users[0].userId, users[1].userId)
        val jwt = getJwt(users[2].login)
        createContacts(UserIdList(userIdList), jwt)
        deleteContacts(UserIdList(userIdList + "invalid-user-id"), jwt).status() shouldBe HttpStatusCode.NoContent
        Contacts.read().userIdList.shouldBeEmpty()
    }

    "Deleting a user should delete it from everyone's contacts" {
        val users = createVerifiedUsers(3)
        val uploadedContacts = UserIdList(setOf(users[1].userId, users[2].userId))
        val jwt = getJwt(users[0].login)
        createContacts(uploadedContacts, jwt)
        deleteUser(getJwt(users[1].login))
        Contacts.read().userIdList shouldContainExactly setOf(users[2].userId)
    }
})

class GetContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be read" {
        val users = createVerifiedUsers(3)
        val userIdList = UserIdList(setOf(users[1].userId, users[2].userId))
        val jwt = getJwt(users[0].login)
        createContacts(userIdList, jwt)
        val response = readContacts(jwt)
        response.status() shouldBe HttpStatusCode.OK
        val body = gson.fromJson(response.content, UserPublicInfoList::class.java)
        val infoList = listOf(users[1], users[2])
            .map { Auth.findUserById(it.userId) }
            .map { UserPublicInfo(it.id, it.username, it.email, it.firstName, it.lastName) }
        body shouldBe UserPublicInfoList(infoList)
    }
})

class PostContactsTest : StringSpec({
    listener(AppListener())

    "Saving previously saved contacts should be ignored" {
        val users = createVerifiedUsers(3)
        val jwt = getJwt(users[0].login)
        val contacts = UserIdList(setOf(users[1].userId, users[2].userId))
        createContacts(contacts, jwt)
        createContacts(contacts, jwt)
        Contacts.read().userIdList shouldContainExactly contacts.userIdList
    }

    "Trying to save the user's own contact should be ignored" {
        val users = createVerifiedUsers(2)
        val contacts = UserIdList(setOf(users[0].userId, users[1].userId))
        val jwt = getJwt(users[0].login)
        val response = createContacts(contacts, jwt)
        response.status() shouldBe HttpStatusCode.NoContent
        Contacts.read(users[0].userId).userIdList shouldContainExactly setOf(users[1].userId)
    }

    "If one of the contacts to be saved is incorrect, then none of them should be saved" {
        val users = createVerifiedUsers(3)
        Auth.deleteUser(users[0].userId)
        val contacts = UserIdList(setOf(users[0].userId, users[2].userId))
        val jwt = getJwt(users[1].login)
        createContacts(contacts, jwt).status() shouldBe HttpStatusCode.BadRequest
        Contacts.read().userIdList.shouldBeEmpty()
    }
})