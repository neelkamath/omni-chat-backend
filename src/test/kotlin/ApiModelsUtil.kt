package com.neelkamath.omniChat

fun AccountInput.toAccount(): Account =
    Account(readUserByUsername(username).id, username, emailAddress, firstName, lastName, bio)

fun buildNewGroupChat(userIdList: List<Int>): GroupChatInput =
    GroupChatInput(GroupChatTitle("T"), GroupChatDescription(""), userIdList.toList())

fun buildNewGroupChat(vararg userIdList: Int): GroupChatInput = buildNewGroupChat(userIdList.toList())