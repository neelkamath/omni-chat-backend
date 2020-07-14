package com.neelkamath.omniChat

fun NewAccount.toAccount(): Account =
    Account(readUserByUsername(username).id, username, emailAddress, firstName, lastName, bio)

fun buildNewGroupChat(userIdList: List<Int>): NewGroupChat =
    NewGroupChat(GroupChatTitle("T"), GroupChatDescription(""), userIdList.toList())

fun buildNewGroupChat(vararg userIdList: Int): NewGroupChat = buildNewGroupChat(userIdList.toList())