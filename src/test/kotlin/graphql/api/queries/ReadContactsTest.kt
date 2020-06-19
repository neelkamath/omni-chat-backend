package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.AccountInfo
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.graphql.api.ACCOUNT_INFO_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createContacts
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CONTACTS_QUERY: String = """
    query ReadContacts {
        readContacts {
            $ACCOUNT_INFO_FRAGMENT
        }
    }
"""

private fun operateReadContacts(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(READ_CONTACTS_QUERY, accessToken = accessToken)

fun readContacts(accessToken: String): List<AccountInfo> {
    val data = operateReadContacts(accessToken).data!!["readContacts"] as List<*>
    return objectMapper.convertValue(data)
}

class ReadContactsTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Contacts should be read") {
        val (admin, contact1, contact2) = createSignedInUsers(3)
        createContacts(admin.accessToken, listOf(contact1.info.id, contact2.info.id))
        readContacts(admin.accessToken) shouldBe listOf(contact1.info, contact2.info)
    }
}