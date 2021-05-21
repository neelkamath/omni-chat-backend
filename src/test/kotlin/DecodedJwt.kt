package com.neelkamath.omniChatBackend

import com.auth0.jwt.interfaces.DecodedJWT
import java.time.LocalDateTime
import java.time.ZoneId

/** The [LocalDateTime] this token expires, or `null` if it doesn't expire. */
val DecodedJWT.expiry: LocalDateTime?
    get() = expiresAt?.let { LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) }
