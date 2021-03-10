package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.toLinkedHashSet
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Returns every contact's cursor in the order of creation. */
fun Contacts.read(): LinkedHashSet<Int> = transaction {
    selectAll().orderBy(Contacts.id).map { it[Contacts.id].value }.toLinkedHashSet()
}
