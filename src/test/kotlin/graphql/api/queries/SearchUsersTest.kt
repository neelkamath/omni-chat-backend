package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.Users
import com.neelkamath.omniChat.db.read
import com.neelkamath.omniChat.graphql.api.ACCOUNTS_CONNECTION_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

const val SEARCH_USERS_QUERY: String = """
    query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchUsers(query: String, pagination: ForwardPagination? = null): GraphQlResponse =
    operateQueryOrMutation(
        SEARCH_USERS_QUERY,
        variables = mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString())
    )

fun searchUsers(query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchUsers(query, pagination).data!!["searchUsers"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class SearchUsersTest : FunSpec({
    test("Users should be searched") {
        val accounts = listOf(
            NewAccount(username = "iron_man", password = "p", emailAddress = "tony@example.com"),
            NewAccount(username = "iron_fist", password = "p", emailAddress = "iron_fist@example.com"),
            NewAccount(username = "hulk", password = "p", emailAddress = "bruce@example.com")
        ).map {
            createAccount(it)
            val id = findUserByUsername(it.username).id
            Account(id, it.username, it.emailAddress, it.firstName, it.lastName)
        }
        searchUsers("iron").edges.map { it.node } shouldContainExactlyInAnyOrder accounts.dropLast(1)
    }

    fun createAccounts(): List<NewAccount> {
        val accounts = listOf(
            NewAccount(username = "iron_man", password = "p", emailAddress = "iron.man@example.com"),
            NewAccount(username = "tony_hawk", password = "p", emailAddress = "tony.hawk@example.com"),
            NewAccount(username = "lol", password = "p", emailAddress = "iron.fist@example.com"),
            NewAccount(username = "another_one", password = "p", emailAddress = "another_one@example.com"),
            NewAccount(username = "jo_mama", password = "p", emailAddress = "mama@example.com", firstName = "Iron"),
            NewAccount(username = "nope", password = "p", emailAddress = "nope@example.com", lastName = "Irony"),
            NewAccount(username = "black_widow", password = "p", emailAddress = "black.widow@example.com"),
            NewAccount(username = "iron_spider", password = "p", emailAddress = "iron.spider@example.com")
        )
        accounts.forEach { createAccount(it) }
        return accounts
    }

    test("Users should be paginated") {
        val infoCursors = createAccounts().zip(Users.read()).map { (newAccount, cursor) ->
            val account = with(newAccount) {
                Account(findUserByUsername(username).id, username, emailAddress, firstName, lastName)
            }
            AccountEdge(account, cursor)
        }
        val searchedUsers = listOf(infoCursors[0], infoCursors[2], infoCursors[4], infoCursors[5], infoCursors[7])
        val first = 3
        val index = 0
        searchUsers("iron", ForwardPagination(first, infoCursors[index].cursor)).edges shouldBe
                searchedUsers.subList(index + 1, index + 1 + first)
    }
})