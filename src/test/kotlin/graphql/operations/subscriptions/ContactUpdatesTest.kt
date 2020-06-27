package com.neelkamath.omniChat.graphql.operations.subscriptions

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.createSignedInUsers
import graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import graphql.operations.DELETED_CONTACT_FRAGMENT
import graphql.operations.NEW_CONTACT_FRAGMENT
import graphql.operations.UPDATED_CONTACT_FRAGMENT
import graphql.operations.mutations.createContacts
import graphql.operations.mutations.deleteAccount
import graphql.operations.mutations.deleteContacts
import graphql.operations.mutations.updateAccount
import graphql.operations.subscriptions.SubscriptionCallback
import graphql.operations.subscriptions.operateGraphQlSubscription
import graphql.operations.subscriptions.parseFrameData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CONTACT_UPDATES_QUERY = """
    subscription ContactUpdates {
        contactUpdates {
            $CREATED_SUBSCRIPTION_FRAGMENT
            $NEW_CONTACT_FRAGMENT
            $UPDATED_CONTACT_FRAGMENT
            $DELETED_CONTACT_FRAGMENT
        }
    }
"""

private fun operateContactUpdates(accessToken: String, callback: SubscriptionCallback): Unit =
    operateGraphQlSubscription(
        uri = "contact-updates",
        request = GraphQlRequest(CONTACT_UPDATES_QUERY),
        accessToken = accessToken,
        callback = callback
    )

fun receiveContactUpdates(accessToken: String, callback: SubscriptionCallback) {
    operateContactUpdates(accessToken) { incoming, outgoing ->
        parseFrameData<CreatedSubscription>(incoming)
        callback(incoming, outgoing)
    }
}

class ContactUpdatesTest : FunSpec({
    test("Subscribers should be notified when they save contacts") {
        val (owner, user1, user2) = createSignedInUsers(3)
        receiveContactUpdates(owner.accessToken) { incoming, _ ->
            createContacts(owner.accessToken, listOf(user1.info.id, user2.info.id))
            parseFrameData<NewContact>(incoming).id shouldBe user1.info.id
            parseFrameData<NewContact>(incoming).id shouldBe user2.info.id
        }
    }

    test("Subscribers should be notified when a user in their contacts updates their account") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        receiveContactUpdates(owner.accessToken) { incoming, _ ->
            val update = AccountUpdate("new_username")
            updateAccount(contact.accessToken, update)
            parseFrameData<UpdatedContact>(incoming).username shouldBe update.username
        }
    }

    test("Subscribers should be notified when they delete contacts") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        receiveContactUpdates(owner.accessToken) { incoming, _ ->
            deleteContacts(owner.accessToken, listOf(contact.info.id))
            parseFrameData<DeletedContact>(incoming).id shouldBe contact.info.id
        }
    }

    test("Subscribers should be notified when a user in their contacts deletes their account") {
        val (owner, contact) = createSignedInUsers(2)
        createContacts(owner.accessToken, listOf(contact.info.id))
        receiveContactUpdates(owner.accessToken) { incoming, _ ->
            deleteAccount(contact.accessToken)
            parseFrameData<DeletedContact>(incoming).id shouldBe contact.info.id
        }
    }
})