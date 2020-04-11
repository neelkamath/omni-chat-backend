package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.Auth

fun Auth.tearDown(): Unit = realm.remove()

fun Auth.verifyEmail(username: String) {
    val user = findUserByUsername(username)
    realm.users().get(user.id).update(user.apply { isEmailVerified = true })
}