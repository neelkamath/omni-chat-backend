package com.neelkamath.omniChat.graphql.api.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

const val CAN_DELETE_ACCOUNT_QUERY: String = """
    query CanDeleteAccount {
        canDeleteAccount
    }
"""

private fun operateCanDeleteAccount(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(CAN_DELETE_ACCOUNT_QUERY, accessToken = accessToken)

fun canDeleteAccount(accessToken: String): Boolean =
    operateCanDeleteAccount(accessToken).data!!["canDeleteAccount"] as Boolean

class CanDeleteAccountTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("An account should be deletable if the user is the admin of an empty group chat") {
        val token = createSignedInUsers(1)[0].accessToken
        createGroupChat(token, NewGroupChat("Title"))
        canDeleteAccount(token).shouldBeTrue()
    }

    test("An account shouldn't be deletable if the user is the admin of a nonempty group chat") {
        val (admin, user) = createSignedInUsers(2)
        createGroupChat(admin.accessToken, NewGroupChat("Title", userIdList = listOf(user.info.id)))
        canDeleteAccount(admin.accessToken).shouldBeFalse()
    }
}