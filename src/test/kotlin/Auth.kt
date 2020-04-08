package com.neelkamath.omniChat

// This file contains extensions for the <Auth> object which are only required by the test source set.

fun Auth.tearDown(): Unit = realm.remove()

fun Auth.verifyEmail(username: String) {
    val user = realm.users().search(username)[0]
    realm.users().get(user.id).update(user.apply { isEmailVerified = true })
}