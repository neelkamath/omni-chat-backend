package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.DELETED_CONTACT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.NEW_CONTACT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_CONTACT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createContacts
import com.neelkamath.omniChat.graphql.operations.mutations.deleteAccount
import com.neelkamath.omniChat.graphql.operations.mutations.deleteContacts
import com.neelkamath.omniChat.graphql.operations.mutations.updateAccount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val SUBSCRIBE_TO_CONTACTS_QUERY = """
    subscription SubscribeToContacts {
        subscribeToContacts {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $NEW_CONTACT_FRAGMENT
            $UPDATED_CONTACT_FRAGMENT
            $DELETED_CONTACT_FRAGMENT
        }
    }
"""

private fun operateSubscribeToContacts(accessToken: String, callback: SubscriptionCallback): Unit =
    operateGraphQlSubscription(
        uri = "subscribe-to-contacts",
        request = GraphQlRequest(SUBSCRIBE_TO_CONTACTS_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun subscribeToContacts(accessToken: String, callback: SubscriptionCallback) {
    operateSubscribeToContacts(accessToken) { incoming ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming)
    }
}

class SubscribeToContactsTest : FunSpec({
    test("Subscribers should be notified when they save contacts") {
        val (owner, user1, user2) = createSignedInUsers(3)
        subscribeToContacts(owner.accessToken) { incoming ->
            createContacts(owner.accessToken, listOf(user1.info.id, user2.info.id))
            parseFrameData<NewContact>(incoming).id shouldBe user1.info.id
            parseFrameData<NewContact>(incoming).id shouldBe user2.info.id
        }
    }

    test("Subscribers should be notified when a user in their contacts updates their account") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        subscribeToContacts(owner.accessToken) { incoming ->
            val update = AccountUpdate(Username("new_username"))
            updateAccount(contact.accessToken, update)
            parseFrameData<UpdatedContact>(incoming).username shouldBe update.username
        }
    }

    test("Subscribers should be notified when they delete contacts") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        subscribeToContacts(owner.accessToken) { incoming ->
            deleteContacts(owner.accessToken, listOf(contact.info.id))
            parseFrameData<DeletedContact>(incoming).id shouldBe contact.info.id
        }
    }

    test("Subscribers should be notified when a user in their contacts deletes their account") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        subscribeToContacts(owner.accessToken) { incoming ->
            deleteAccount(contact.accessToken)
            parseFrameData<DeletedContact>(incoming).id shouldBe contact.info.id
        }
    }

    test("The subscriptions should be stopped if the user deletes their account") {
        val token = createSignedInUsers(1)[0].accessToken
        subscribeToContacts(token) { incoming ->
            deleteAccount(token)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})