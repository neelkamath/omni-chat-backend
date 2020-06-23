package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.api.ACCOUNTS_CONNECTION_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.graphql.api.mutations.createContacts
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEARCH_CONTACTS_QUERY: String = """
    query SearchContacts(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchContacts(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchContacts(
    accessToken: String,
    query: String,
    pagination: ForwardPagination? = null
): GraphQlResponse = operateQueryOrMutation(
    SEARCH_CONTACTS_QUERY,
    variables = mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
    accessToken = accessToken
)

fun searchContacts(accessToken: String, query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchContacts(accessToken, query, pagination).data!!["searchContacts"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class SearchContactsTest : FunSpec({
    test("Contacts should be searched case-insensitively") {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "p", emailAddress = "john.doe@example.com"),
            NewAccount(username = "john_roger", password = "p", emailAddress = "john.roger@example.com"),
            NewAccount(username = "nick_bostrom", password = "p", emailAddress = "nick.bostrom@example.com"),
            NewAccount(username = "iron_man", password = "p", emailAddress = "roger@example.com", firstName = "John")
        ).map {
            createAccount(it)
            val userId = findUserByUsername(it.username).id
            Account(userId, it.username, it.emailAddress, it.firstName, it.lastName)
        }
        val token = createSignedInUsers(1)[0].accessToken
        createContacts(token, accounts.map { it.id })
        val testContacts = { query: String, accountList: List<Account> ->
            searchContacts(token, query).edges.map { it.node } shouldBe accountList
        }
        testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
        testContacts("bost", listOf(accounts[2]))
        testContacts("Roger", listOf(accounts[1], accounts[3]))
    }

    test("Contacts should be paginated") { testContactsPagination(ContactsOperationName.SEARCH_CONTACTS) }
})