package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ContactsAsset
import com.neelkamath.omniChat.db.contactsBroker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.reactivex.rxjava3.subscribers.TestSubscriber

class UsersTest : FunSpec({
    context("deleteProfilePic(String)") {
        test("Deleting a profile pic should remove it") {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.setProfilePic(userId)
            Users.deleteProfilePic(userId)
            Users.readProfilePic(userId).shouldBeNull()
        }

        test("An update shouldn't be sent if the user deleted a profile pic they never had") {
            val (contactId, contactOwnerId) = createVerifiedUsers(2).map { it.info.id }
            val subscriber = contactsBroker.subscribe(ContactsAsset(contactOwnerId)).subscribeWith(TestSubscriber())
            Users.deleteProfilePic(contactId)
            subscriber.assertNoValues()
        }
    }
})