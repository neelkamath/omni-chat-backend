package com.neelkamath.omniChat.test.graphql.api.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
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

class CanDeleteAccountTest : FunSpec({
    test("An account should be able to be deleted if the user is the admin of an empty group chat") {
        val token = createVerifiedUsers(1)[0].accessToken
        createGroupChat(NewGroupChat("Title"), token)
        canDeleteAccount(token).shouldBeTrue()
    }

    test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
        val (admin, user) = createVerifiedUsers(2)
        createGroupChat(NewGroupChat("Title", userIdList = setOf(user.info.id)), admin.accessToken)
        canDeleteAccount(admin.accessToken).shouldBeFalse()
    }
})