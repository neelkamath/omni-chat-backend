package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class PostRefreshJwtTest : StringSpec({
    listener(AppListener())

    "A refresh token should issue a new token set" {
        val login = Login("username", "password")
        Server.createAccount(User(login, "username@gmail.com"))
        Auth.verifyEmail(login.username!!)
        val token = gson.fromJson(Server.requestJwt(login).content, AuthToken::class.java).refreshToken
        val response = Server.refreshJwt(token)
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, AuthToken::class.java) // Successfully parsing it verifies the response body.
    }
})