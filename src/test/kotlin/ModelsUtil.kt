package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle

fun Account.toUpdatedAccount(): UpdatedAccount = UpdatedAccount(username, emailAddress, firstName, lastName)

fun buildNewGroupChat(userIdList: List<String>): NewGroupChat =
    NewGroupChat(GroupChatTitle("T"), GroupChatDescription(""), userIdList.toList())

fun buildNewGroupChat(vararg userIdList: String): NewGroupChat = buildNewGroupChat(userIdList.toList())