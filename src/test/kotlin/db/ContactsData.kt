package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.Contacts
import com.neelkamath.omniChat.db.ContactsData
import com.neelkamath.omniChat.db.DB
import org.jetbrains.exposed.sql.selectAll

fun readContacts(): Contacts = DB.transact {
    Contacts(ContactsData.Table.selectAll().map { it[ContactsData.Table.contact] }.toSet())
}