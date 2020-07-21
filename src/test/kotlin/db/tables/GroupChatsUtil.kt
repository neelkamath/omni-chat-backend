package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.buildNewGroupChat

fun GroupChats.create(adminId: Int): Int = create(adminId, buildNewGroupChat())