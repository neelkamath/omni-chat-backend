package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.graphql.InvalidContactException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val CREATE_CONTACTS_QUERY: String = """
    mutation CreateContacts(${"$"}userIdList: [String!]!) {
        createContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateCreateContacts(accessToken: String, userIdList: List<String>): GraphQlResponse =
    operateQueryOrMutation(
        CREATE_CONTACTS_QUERY,
        variables = mapOf("userIdList" to userIdList),
        accessToken = accessToken
    )

fun createContacts(accessToken: String, userIdList: List<String>): Boolean =
    operateCreateContacts(accessToken, userIdList).data!!["createContacts"] as Boolean

fun errCreateContacts(accessToken: String, userIdList: List<String>): String =
    operateCreateContacts(accessToken, userIdList).errors!![0].message

class CreateContactsTest : FunSpec({
    test("Trying to save the user's own contact should be ignored") {
        val (owner, user) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(owner.info.id, user.info.id))
        Contacts.readIdList(owner.info.id) shouldBe listOf(user.info.id)
    }

    test("If one of the contacts to be saved is invalid, then none of them should be saved") {
        val (owner, user) = createSignedInUsers(2)
        val contacts = listOf(user.info.id, "invalid user ID")
        errCreateContacts(owner.accessToken, contacts) shouldBe InvalidContactException.message
        Contacts.readIdList(owner.info.id).shouldBeEmpty()
    }
})