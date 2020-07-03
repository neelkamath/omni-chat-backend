package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.AccountsConnection
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_CONNECTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createContacts
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CONTACTS_QUERY = """
    query ReadContacts(${"$"}first: Int, ${"$"}after: Cursor) {
        readContacts(first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateReadContacts(accessToken: String, pagination: ForwardPagination? = null): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        READ_CONTACTS_QUERY,
        variables = mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        accessToken = accessToken
    )

fun readContacts(accessToken: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateReadContacts(accessToken, pagination).data!!["readContacts"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class ReadContactsTest : FunSpec({
    test("Contacts should be read") {
        val (owner, contact1, contact2) = createSignedInUsers(3)
        createContacts(owner.accessToken, listOf(contact1.info.id, contact2.info.id))
        readContacts(owner.accessToken).edges.map { it.node } shouldBe listOf(contact1.info, contact2.info)
    }

    test("Contacts should be paginated") { testContactsPagination(ContactsOperationName.READ_CONTACTS) }
})