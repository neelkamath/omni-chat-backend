package com.neelkamath.omniChat

fun Account.toUpdatedAccount(): UpdatedAccount = UpdatedAccount(username, emailAddress, firstName, lastName)