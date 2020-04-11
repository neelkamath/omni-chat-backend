package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun searchUsers(query: UserSearchQuery): TestApplicationResponse = withTestApplication(Application::main) {
    val map = mutableMapOf<String, String>()
    with(query) {
        if (username != null) map["username"] = username!!
        if (email != null) map["email"] = email!!
        if (firstName != null) map["first_name"] = firstName!!
        if (lastName != null) map["last_name"] = lastName!!
    }
    val parameters = map.toList().formUrlEncode()
    handleRequest(HttpMethod.Get, "/search-users?$parameters")
}.response

class GetSearchUsersTest : StringSpec({
    listener(AppListener())

    fun createUsers(): UserPublicInfoList = UserPublicInfoList(
        listOf(
            NewUser(username = "tony", password = "p", email = "tony@example.com", firstName = "Tony"),
            NewUser(username = "johndoe", password = "j", email = "john@example.com", firstName = "John"),
            NewUser(username = "john.rogers", password = "r", email = "rogers@example.com"),
            NewUser(username = "anonymous", password = "a", email = "anon@example.com", firstName = "John")
        ).map {
            createUser(it)
            val userId = Auth.findUserByUsername(it.username).id
            UserPublicInfo(userId, it.username, it.email, it.firstName, it.lastName)
        }
    )

    fun search(query: UserSearchQuery): UserPublicInfoList =
        gson.fromJson(searchUsers(query).content, UserPublicInfoList::class.java)

    "Search results should use the provided filters" {
        val users = createUsers().users
        val usernameResults = search(UserSearchQuery(username = "john"))
        usernameResults.users shouldContainExactlyInAnyOrder listOf(users[1], users[2])
        val nameResults = search(UserSearchQuery(firstName = "john"))
        nameResults.users shouldContainExactlyInAnyOrder listOf(users[1], users[3])
        val combinedResults = search(UserSearchQuery(username = "john", firstName = "john"))
        combinedResults.users shouldContainExactlyInAnyOrder listOf(users[1])
    }

    "Sending at least one filter should be made mandatory" {
        searchUsers(UserSearchQuery()).status() shouldBe HttpStatusCode.BadRequest
    }
})