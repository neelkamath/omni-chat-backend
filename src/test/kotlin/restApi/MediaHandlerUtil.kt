package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.test
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*

/** Creates a [file] which doesn't get saved to the filesystem. An example [name] is `"pic.png"`. */
data class DummyFile(val name: String, val bytes: Int) {
    val file = ByteArray(bytes)
}

fun getFileMessage(accessToken: String, messageId: Int, path: String): TestApplicationResponse =
        withTestApplication(Application::test) {
            val parameters = listOf("message-id" to messageId.toString()).formUrlEncode()
            handleRequest(HttpMethod.Get, "$path?$parameters") {
                addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
            }.response
        }

fun uploadFile(
        accessToken: String,
        dummy: DummyFile,
        method: HttpMethod,
        path: String,
        parameters: String? = null
): TestApplicationResponse = withTestApplication(Application::test) {
    handleRequest(method, if (parameters == null) path else "$path?$parameters") {
        addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        val boundary = "boundary"
        addHeader(
                HttpHeaders.ContentType,
                ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
        )
        setBody(
                boundary,
                listOf(
                        PartData.FileItem(
                                { dummy.file.inputStream().asInput() },
                                {},
                                headersOf(
                                        HttpHeaders.ContentDisposition,
                                        ContentDisposition.File
                                                .withParameter(ContentDisposition.Parameters.Name, "file")
                                                .withParameter(ContentDisposition.Parameters.FileName, dummy.name)
                                                .toString()
                                )
                        )
                )
        )
    }.response
}
