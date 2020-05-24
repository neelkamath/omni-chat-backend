package com.neelkamath.omniChat.test.graphql.api

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.queries.READ_ACCOUNT_QUERY
import com.neelkamath.omniChat.test.graphql.api.queries.REQUEST_TOKEN_SET_QUERY
import com.neelkamath.omniChat.test.graphql.api.queries.requestTokenSet
import com.neelkamath.omniChat.test.verifyEmailAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotHaveKey

/**
 * These tests verify that the data the client receives conforms to the GraphQL spec.
 *
 * We use a GraphQL library which returns data according to the spec. However, there are two caveats which can cause the
 * data the client receives to be non-compliant with the spec:
 * 1. The GraphQL library returns `null` values for the `"data"` and `"errors"` keys. The spec mandates that these keys
 * keys either be non-`null` or not be returned at all.
 * 1. Data is serialized as JSON using the [objectMapper]. The [objectMapper] which may remove `null` fields if
 * incorrectly configured. The spec mandates that requested fields be returned even if they're `null`.
 */
class SpecComplianceTest : FunSpec({
    test("""The "data" key shouldn't be returned if there was no data to be received""") {
        val variables = mapOf("login" to Login("username", "password"))
        queryOrMutate(REQUEST_TOKEN_SET_QUERY, variables) shouldNotHaveKey "data"
    }

    test("""The "errors" key shouldn't be returned if there were no errors""") {
        val login = createVerifiedUsers(1)[0].login
        queryOrMutate(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login)) shouldNotHaveKey "errors"
    }

    test("""null fields in the "data" key should be returned""") {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        verifyEmailAddress(account.username)
        val login = Login(account.username, account.password)
        val accessToken = requestTokenSet(login).accessToken
        val response = queryOrMutate(READ_ACCOUNT_QUERY, accessToken = accessToken)["data"] as Map<*, *>
        objectMapper.convertValue<Map<String, Any>>(response["readAccount"]!!) shouldContain Pair("firstName", null)
    }
})