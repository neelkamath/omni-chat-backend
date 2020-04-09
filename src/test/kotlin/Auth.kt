package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.Auth

fun Auth.tearDown(): Unit = realm.remove()

fun Auth.verifyEmail(username: String) {
    val user = realm.users().search(username)[0]
    realm.users().get(user.id).update(user.apply { isEmailVerified = true })
}