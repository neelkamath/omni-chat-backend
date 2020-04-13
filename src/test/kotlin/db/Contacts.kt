package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.db.DB
import org.jetbrains.exposed.sql.selectAll

/** Returns the user ID list of the saved contacts of every user. */
fun Contacts.read(): Set<String> = DB.transact {
    selectAll().map { it[contactUserId] }.toSet()
}