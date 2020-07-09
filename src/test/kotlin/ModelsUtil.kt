package com.neelkamath.omniChat

fun buildNewGroupChat(userIdList: List<String>): NewGroupChat =
    NewGroupChat(GroupChatTitle("T"), GroupChatDescription(""), userIdList.toList())

fun buildNewGroupChat(vararg userIdList: String): NewGroupChat = buildNewGroupChat(userIdList.toList())