package com.neelkamath.omniChat.graphql.operations.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

const val CAN_DELETE_ACCOUNT_QUERY = """
    query CanDeleteAccount {
        canDeleteAccount
    }
"""

private fun operateCanDeleteAccount(accessToken: String): GraphQlResponse =
    operateGraphQlQueryOrMutation(CAN_DELETE_ACCOUNT_QUERY, accessToken = accessToken)

fun canDeleteAccount(accessToken: String): Boolean =
    operateCanDeleteAccount(accessToken).data!!["canDeleteAccount"] as Boolean

class CanDeleteAccountTest : FunSpec({
    test("An account should be deletable if the user is the admin of an empty group chat") {
        val user = createVerifiedUsers(1)[0]
        GroupChats.create(user.info.id, buildNewGroupChat())
        canDeleteAccount(user.accessToken).shouldBeTrue()
    }

    test("An account shouldn't be deletable if the user is the admin of a nonempty group chat") {
        val (admin, user) = createVerifiedUsers(2)
        GroupChats.create(admin.info.id, buildNewGroupChat(user.info.id))
        canDeleteAccount(admin.accessToken).shouldBeFalse()
    }
})