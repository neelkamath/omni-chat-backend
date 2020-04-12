package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.db.DB
import org.jetbrains.exposed.sql.selectAll

fun Contacts.read(): UserIdList = DB.transact {
    UserIdList(selectAll().map { it[contact] }.toSet())
}