package com.neelkamath.omniChat

fun AccountInput.toAccount(): Account =
    Account(readUserByUsername(username).id, username, emailAddress, firstName, lastName, bio)