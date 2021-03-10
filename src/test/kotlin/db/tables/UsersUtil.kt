package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.toLinkedHashSet
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Every user's cursor (ID) in their order of creation. */
fun Users.read(): LinkedHashSet<Int> = transaction {
    selectAll().orderBy(Users.id).map { it[Users.id].value }.toLinkedHashSet()
}
