package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.graphql.api.buildAccountInfoFragment
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

fun buildSearchUsersQuery(): String = """
    query SearchUsers(${"$"}query: String!) {
        searchUsers(query: ${"$"}query) {
            ${buildAccountInfoFragment()}
        }
    }
"""

private fun operateSearchUsers(query: String): GraphQlResponse =
    operateQueryOrMutation(buildSearchUsersQuery(), variables = mapOf("query" to query))

fun searchUsers(query: String): List<AccountInfo> {
    val data = operateSearchUsers(query).data!!["searchUsers"] as List<*>
    return objectMapper.convertValue(data)
}

class SearchUsersTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Users should be searched") {
        val accounts = listOf(
            NewAccount(username = "iron_man", password = "p", emailAddress = "tony@example.com"),
            NewAccount(username = "iron_fist", password = "p", emailAddress = "iron_fist@example.com"),
            NewAccount(username = "hulk", password = "p", emailAddress = "bruce@example.com")
        )
        val infoList = accounts.map {
            createAccount(it)
            val id = findUserByUsername(it.username).id
            AccountInfo(id, it.username, it.emailAddress, it.firstName, it.lastName)
        }
        searchUsers("iron") shouldContainExactlyInAnyOrder infoList.dropLast(1)
    }
}