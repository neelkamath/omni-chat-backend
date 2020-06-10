package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.graphql.api.buildAccountInfoFragment
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.mutations.createContacts
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

fun buildSearchContactsQuery(): String = """
    query SearchContacts(${"$"}query: String!) {
        searchContacts(query: ${"$"}query) {
            ${buildAccountInfoFragment()}
        }
    }
"""

private fun operateSearchContacts(accessToken: String, query: String): GraphQlResponse =
    operateQueryOrMutation(buildSearchContactsQuery(), variables = mapOf("query" to query), accessToken = accessToken)

fun searchContacts(accessToken: String, query: String): List<AccountInfo> {
    val data = operateSearchContacts(accessToken, query).data!!["searchContacts"] as List<*>
    return objectMapper.convertValue(data)
}

class SearchContactsTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Contacts should be searched case-insensitively") {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "p", emailAddress = "john.doe@example.com"),
            NewAccount(username = "john_roger", password = "p", emailAddress = "john.roger@example.com"),
            NewAccount(username = "nick_bostrom", password = "p", emailAddress = "nick.bostrom@example.com"),
            NewAccount(username = "iron_man", password = "p", emailAddress = "roger@example.com", firstName = "John")
        ).map {
            createAccount(it)
            val userId = findUserByUsername(it.username).id
            AccountInfo(userId, it.username, it.emailAddress, it.firstName, it.lastName)
        }
        val token = createSignedInUsers(1)[0].accessToken
        createContacts(token, accounts.map { it.id })
        searchContacts(token, "john") shouldBe listOf(accounts[0], accounts[1], accounts[3])
        searchContacts(token, "bost") shouldBe listOf(accounts[2])
        searchContacts(token, "Roger") shouldBe listOf(accounts[1], accounts[3])
    }
}