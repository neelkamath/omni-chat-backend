package graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.contacts.Contacts
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
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

fun deleteContacts(accessToken: String, userIdList: List<String>) =
    operateDeleteContacts(accessToken, userIdList).data!!["deleteContacts"]

class DeleteContactsTest : FunSpec({
    test("Contacts should be deleted, ignoring invalid ones") {
        val (owner, user1, user2) = createSignedInUsers(3)
        val userIdList = listOf(user1.info.id, user2.info.id)
        createContacts(owner.accessToken, userIdList)
        deleteContacts(owner.accessToken, userIdList + "invalid user id")
        Contacts.readIdList(owner.info.id).shouldBeEmpty()
    }
})