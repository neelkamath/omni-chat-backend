package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.User
import com.neelkamath.omniChat.gson
import com.neelkamath.omniChat.main
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

object Server {
    fun checkHealth(): TestApplicationResponse =
        withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "/health_check") }.response

    fun requestJwt(login: Login): TestApplicationResponse = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "/jwt") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(login))
        }
    }.response

    fun refreshJwt(refreshToken: String): TestApplicationResponse = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "/jwt_refresh") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(listOf("refresh_token" to refreshToken).formUrlEncode())
        }
    }.response

    fun createAccount(user: User): TestApplicationResponse = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "/user") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(user))
        }
    }.response

    fun updateAccount(update: User, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "/user") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

    fun readAccount(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "/user") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
    }.response
}