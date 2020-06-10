package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.MessagesConnection
import com.neelkamath.omniChat.PageInfo

val emptyMessagesConnection = MessagesConnection(
    edges = listOf(),
    pageInfo = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
)