package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.ContactsData
import com.neelkamath.omniChat.db.DB
import org.jetbrains.exposed.sql.selectAll

fun readContacts(): UserIdList = DB.transact {
    UserIdList(ContactsData.Table.selectAll().map { it[ContactsData.Table.contact] }.toSet())
}