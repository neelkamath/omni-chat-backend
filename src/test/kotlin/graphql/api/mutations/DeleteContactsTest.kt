package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

fun buildDeleteContactsQuery(): String = """
    mutation DeleteContacts(${"$"}userIdList: [String!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateDeleteContacts(accessToken: String, userIdList: List<String>): GraphQlResponse =
    operateQueryOrMutation(
        buildDeleteContactsQuery(),
        variables = mapOf("userIdList" to userIdList),
        accessToken = accessToken
    )

fun deleteContacts(accessToken: String, userIdList: List<String>): Boolean =
    operateDeleteContacts(accessToken, userIdList).data!!["deleteContacts"] as Boolean

class DeleteContactsTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Contacts should be deleted, ignoring invalid ones") {
        val (owner, user1, user2) = createSignedInUsers(3)
        val userIdList = listOf(user1.info.id, user2.info.id)
        createContacts(owner.accessToken, userIdList)
        deleteContacts(owner.accessToken, userIdList + "invalid user id")
        Contacts.read(owner.info.id).shouldBeEmpty()
    }
}