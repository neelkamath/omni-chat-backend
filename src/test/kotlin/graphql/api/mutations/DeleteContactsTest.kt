package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

const val DELETE_CONTACTS_QUERY: String = """
    mutation DeleteContacts(${"$"}userIdList: [String!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateDeleteContacts(userIdList: List<String>, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(
        DELETE_CONTACTS_QUERY,
        variables = mapOf("userIdList" to userIdList),
        accessToken = accessToken
    )

fun deleteContacts(userIdList: List<String>, accessToken: String): Boolean =
    operateDeleteContacts(userIdList, accessToken).data!!["deleteContacts"] as Boolean

class DeleteContactsTest : FunSpec({
    listener(AppListener())

    test("Contacts should be deleted, ignoring invalid ones") {
        val (owner, user1, user2) = createVerifiedUsers(3)
        val userIdList = listOf(user1.info.id, user2.info.id)
        createContacts(userIdList, owner.accessToken)
        deleteContacts(userIdList + "invalid user id", owner.accessToken)
        Contacts.read(owner.info.id).shouldBeEmpty()
    }
})