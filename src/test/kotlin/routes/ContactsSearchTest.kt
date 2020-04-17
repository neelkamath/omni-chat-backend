package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun searchContacts(query: String, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("query", query) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "contacts-search?$parameters") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class GetContactsSearchTest : StringSpec({
    listener(AppListener())

    fun testResponse(response: TestApplicationResponse, userIdList: Set<String>) {
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, UserIdList::class.java) shouldBe UserIdList(userIdList)
    }

    "Contacts should be searched case-insensitively" {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "p", email = "john.doe@example.com"),
            NewAccount(username = "john_roger", password = "p", email = "john.roger@example.com"),
            NewAccount(username = "nick_bostrom", password = "p", email = "nick.bostrom@example.com"),
            NewAccount(username = "iron_man_fan", password = "p", email = "roger@example.com", firstName = "John")
        )
        for (account in accounts) createAccount(account)
        val userIdList = accounts.map { Auth.findUserByUsername(it.username) }.map { it.id }
        val jwt = getJwt(createVerifiedUsers(1)[0].login)
        createContacts(UserIdList(userIdList.toSet()), jwt)
        testResponse(searchContacts("john", jwt), setOf(userIdList[0], userIdList[1], userIdList[3]))
        testResponse(searchContacts("bost", jwt), setOf(userIdList[2]))
        testResponse(searchContacts("Roger", jwt), setOf(userIdList[1], userIdList[3]))
    }
})