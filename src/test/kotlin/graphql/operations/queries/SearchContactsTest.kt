package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_CONNECTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEARCH_CONTACTS_QUERY = """
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
): GraphQlResponse = operateGraphQlQueryOrMutation(
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
            NewAccount(Username("john_doe"), Password("p"), emailAddress = "john.doe@example.com"),
            NewAccount(Username("john_roger"), Password("p"), emailAddress = "john.roger@example.com"),
            NewAccount(Username("nick_bostrom"), Password("p"), emailAddress = "nick.bostrom@example.com"),
            NewAccount(Username("iron_man"), Password("p"), emailAddress = "roger@example.com", firstName = "John")
        ).map {
            createUser(it)
            val userId = readUserByUsername(it.username).id
            Account(userId, it.username, it.emailAddress, it.bio, it.firstName, it.lastName)
        }
        val user = createVerifiedUsers(1)[0]
        Contacts.create(user.info.id, accounts.map { it.id }.toSet())
        val testContacts = { query: String, accountList: List<Account> ->
            searchContacts(user.accessToken, query).edges.map { it.node } shouldBe accountList
        }
        testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
        testContacts("bost", listOf(accounts[2]))
        testContacts("Roger", listOf(accounts[1], accounts[3]))
    }

    test("Contacts should be paginated") { testContactsPagination(ContactsOperationName.SEARCH_CONTACTS) }
})