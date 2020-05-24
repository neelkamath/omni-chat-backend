package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ContactsTest : FunSpec({
    context("create(String, Set<String>)") {
        test("Saving contacts should ignore existing contacts") {
            val (userId, contact1Id, contact2Id, contact3Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            Contacts.read(userId) shouldBe setOf(contact1Id, contact2Id, contact3Id)
        }
    }
})