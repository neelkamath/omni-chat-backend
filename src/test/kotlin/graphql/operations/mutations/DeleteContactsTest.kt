package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.Placeholder
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

const val DELETE_CONTACTS_QUERY = """
    mutation DeleteContacts(${"$"}userIdList: [String!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateDeleteContacts(accessToken: String, userIdList: List<String>): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        DELETE_CONTACTS_QUERY,
        variables = mapOf("userIdList" to userIdList),
        accessToken = accessToken
    )

fun deleteContacts(accessToken: String, userIdList: List<String>): Placeholder {
    val data = operateDeleteContacts(accessToken, userIdList).data!!["deleteContacts"] as String
    return objectMapper.convertValue(data)
}

class DeleteContactsTest : FunSpec({
    test("Contacts should be deleted, ignoring invalid ones") {
        val (owner, user1, user2) = createVerifiedUsers(3)
        val userIdList = listOf(user1.info.id, user2.info.id)
        Contacts.create(owner.info.id, userIdList.toSet())
        deleteContacts(owner.accessToken, userIdList + "invalid user id")
        Contacts.readIdList(owner.info.id).shouldBeEmpty()
    }
})