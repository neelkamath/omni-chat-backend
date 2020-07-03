package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

/** Returns every contact's cursor in the order of creation. */
fun Contacts.read(): List<Int> = transact {
    selectAll().map { it[Contacts.id].value }
}