package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.AuthToken
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.gson

fun getJwt(login: Login): String {
    val tokenSet = Server.requestJwt(login).content
    return gson.fromJson(tokenSet, AuthToken::class.java).jwt
}