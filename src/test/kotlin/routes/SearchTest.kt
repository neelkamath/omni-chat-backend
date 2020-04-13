package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun searchUsers(query: UserSearchQuery): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = with(query) {
        Parameters.build {
            if (username != null) append("username", username!!)
            if (email != null) append("email", email!!)
            if (firstName != null) append("first_name", firstName!!)
            if (lastName != null) append("last_name", lastName!!)
        }.formUrlEncode()
    }
    handleRequest(HttpMethod.Get, "user-search?$parameters")
}.response

fun readUser(userId: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("user_id", userId) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "user?$parameters")
}.response

class GetUserSearchTest : StringSpec({
    listener(AppListener())

    fun createUsers(): UserIdList = UserIdList(
        listOf(
            NewAccount(username = "tony", password = "p", email = "tony@example.com", firstName = "Tony"),
            NewAccount(username = "johndoe", password = "j", email = "john@example.com", firstName = "John"),
            NewAccount(username = "john.rogers", password = "r", email = "rogers@example.com"),
            NewAccount(username = "anonymous", password = "a", email = "anon@example.com", firstName = "John")
        )
            .map {
                createAccount(it)
                Auth.findUserByUsername(it.username).id
            }
            .toSet()
    )

    fun search(query: UserSearchQuery): UserIdList = gson.fromJson(searchUsers(query).content, UserIdList::class.java)

    "Search results should use the provided filters" {
        val userIdList = createUsers().userIdList
        val usernameResults = search(UserSearchQuery(username = "john"))
        usernameResults shouldBe UserIdList(setOf(userIdList.elementAt(1), userIdList.elementAt(2)))
        val nameResults = search(UserSearchQuery(firstName = "john"))
        nameResults shouldBe UserIdList(setOf(userIdList.elementAt(1), userIdList.elementAt(3)))
        val combinedResults = search(UserSearchQuery(username = "john", firstName = "john"))
        combinedResults shouldBe UserIdList(setOf(userIdList.elementAt(1)))
    }

    "Sending at least one filter should be made mandatory" {
        searchUsers(UserSearchQuery()).status() shouldBe HttpStatusCode.BadRequest
    }
})

class GetUserTest : StringSpec({
    listener(AppListener())

    "A user's info should be retrieved" {
        val account = NewAccount("username", "password", "username@example.com", "first name")
        createAccount(account)
        val response = readUser(Auth.findUserByUsername(account.username).id)
        response.status() shouldBe HttpStatusCode.OK
        val body = gson.fromJson(response.content, User::class.java)
        body shouldBe User(account.username, account.email, account.firstName, account.lastName)
    }

    "Requesting the details of a nonexistent user should respond with an HTTP status code of 400" {
        readUser("a nonexistent user ID").status() shouldBe HttpStatusCode.BadRequest
    }
})