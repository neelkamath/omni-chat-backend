package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.ACCOUNT_INFO_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.mutations.createContacts
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEARCH_CONTACTS_QUERY: String = """
    query SearchContacts(${"$"}query: String!) {
        searchContacts(query: ${"$"}query) {
            $ACCOUNT_INFO_FRAGMENT
        }
    }
"""

private fun operateSearchContacts(query: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(SEARCH_CONTACTS_QUERY, variables = mapOf("query" to query), accessToken = accessToken)

fun searchContacts(query: String, accessToken: String): List<AccountInfo> {
    val data = operateSearchContacts(query, accessToken).data!!["searchContacts"] as List<*>
    return jsonMapper.convertValue(data)
}

class SearchContactsTest : FunSpec({
    listener(AppListener())

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
        val token = createVerifiedUsers(1)[0].accessToken
        createContacts(accounts.map { it.id }, token)
        searchContacts("john", token) shouldBe listOf(accounts[0], accounts[1], accounts[3])
        searchContacts("bost", token) shouldBe listOf(accounts[2])
        searchContacts("Roger", token) shouldBe listOf(accounts[1], accounts[3])
    }
})