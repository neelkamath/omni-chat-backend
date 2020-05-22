package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Contacts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ContactsTest : FunSpec({
    listener(DbListener())

    context("create(String, Set<String>)") {
        test("Saving contacts should ignore existing contacts") {
            val userId = "user ID"
            val contact1Id = "contact 1 ID"
            val contact2Id = "contact 2 ID"
            val contact3Id = "contact 3 ID"
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            Contacts.read(userId) shouldBe setOf(contact1Id, contact2Id, contact3Id)
        }
    }
})