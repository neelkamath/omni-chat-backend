package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_CONNECTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

const val SEARCH_USERS_QUERY = """
    query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchUsers(query: String, pagination: ForwardPagination? = null): GraphQlResponse =
    operateGraphQlQueryOrMutation(
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
            NewAccount(Username("iron_man"), Password("p"), "tony@example.com"),
            NewAccount(Username("iron_fist"), Password("p"), "iron_fist@example.com"),
            NewAccount(Username("hulk"), Password("p"), "bruce@example.com")
        ).map {
            createUser(it)
            val id = readUserByUsername(it.username).id
            Account(id, it.username, it.emailAddress, it.bio, it.firstName, it.lastName)
        }
        searchUsers("iron").edges.map { it.node } shouldContainExactlyInAnyOrder accounts.dropLast(1)
    }

    fun createAccounts(): List<NewAccount> {
        val accounts = listOf(
            NewAccount(Username("iron_man"), Password("p"), "iron.man@example.com"),
            NewAccount(Username("tony_hawk"), Password("p"), "tony.hawk@example.com"),
            NewAccount(Username("lol"), Password("p"), "iron.fist@example.com"),
            NewAccount(Username("another_one"), Password("p"), "another_one@example.com"),
            NewAccount(Username("jo_mama"), Password("p"), "mama@example.com", firstName = "Iron"),
            NewAccount(Username("nope"), Password("p"), "nope@example.com", lastName = "Irony"),
            NewAccount(Username("black_widow"), Password("p"), "black.widow@example.com"),
            NewAccount(Username("iron_spider"), Password("p"), "iron.spider@example.com")
        )
        accounts.forEach(::createUser)
        return accounts
    }

    test("Users should be paginated") {
        val infoCursors = createAccounts().zip(Users.read()).map { (newAccount, cursor) ->
            val account = with(newAccount) {
                Account(readUserByUsername(username).id, username, emailAddress, bio, firstName, lastName)
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