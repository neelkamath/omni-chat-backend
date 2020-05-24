package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.graphql.InvalidContactException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val CREATE_CONTACTS_QUERY: String = """
    mutation CreateContacts(${"$"}userIdList: [String!]!) {
        createContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateCreateContacts(userIdList: List<String>, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(
        CREATE_CONTACTS_QUERY,
        variables = mapOf("userIdList" to userIdList),
        accessToken = accessToken
    )

fun createContacts(userIdList: List<String>, accessToken: String): Boolean =
    operateCreateContacts(userIdList, accessToken).data!!["createContacts"] as Boolean

fun errCreateContacts(userIdList: List<String>, accessToken: String): String =
    operateCreateContacts(userIdList, accessToken).errors!![0].message

class CreateContactsTest : FunSpec({
    test("Trying to save the user's own contact should be ignored") {
        val (owner, user) = createVerifiedUsers(2)
        createContacts(listOf(owner.info.id, user.info.id), owner.accessToken)
        Contacts.read(owner.info.id) shouldBe setOf(user.info.id)
    }

    test("If one of the contacts to be saved is invalid, then none of them should be saved") {
        val (owner, user) = createVerifiedUsers(2)
        val contacts = listOf(user.info.id, "invalid user ID")
        errCreateContacts(contacts, owner.accessToken) shouldBe InvalidContactException().message
        Contacts.read(owner.info.id).shouldBeEmpty()
    }
})