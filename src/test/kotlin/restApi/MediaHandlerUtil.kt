package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*

/** Creates a [file] which doesn't get saved to the filesystem. An example [name] is `"pic.png"`. */
data class DummyFile(val name: String, val bytes: Int) {
    val file = ByteArray(bytes)
}

fun getFileMessage(
    accessToken: String? = null,
    path: String,
    messageId: Int,
    picType: PicType? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = listOf("message-id" to messageId.toString(), "pic-type" to picType?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    handleRequest(HttpMethod.Get, "$path?$parameters") {
        if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
    }.response
}

fun uploadMultipart(
    accessToken: String,
    method: HttpMethod,
    path: String,
    parts: List<PartData>,
    parameters: String? = null,
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(method, if (parameters == null) path else "$path?$parameters") {
        addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        val boundary = "boundary"
        addHeader(
            HttpHeaders.ContentType,
            ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString(),
        )
        setBody(boundary, parts)
    }.response
}

fun buildFileItem(filename: String, fileContent: ByteArray): PartData.FileItem = PartData.FileItem(
    { fileContent.inputStream().asInput() },
    {},
    headersOf(
        HttpHeaders.ContentDisposition,
        ContentDisposition.File
            .withParameter(ContentDisposition.Parameters.Name, "file")
            .withParameter(ContentDisposition.Parameters.FileName, filename)
            .toString(),
    ),
)

fun buildFormItem(name: String, value: String): PartData.FormItem = PartData.FormItem(
    value,
    {},
    headersOf(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, name).toString(),
    ),
)

fun uploadFile(
    accessToken: String,
    filename: String,
    fileContent: ByteArray,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
): TestApplicationResponse =
    uploadMultipart(accessToken, method, path, listOf(buildFileItem(filename, fileContent)), parameters)

fun uploadFile(
    accessToken: String,
    dummy: DummyFile,
    method: HttpMethod,
    path: String,
    parameters: String? = null,
): TestApplicationResponse = uploadFile(accessToken, dummy.name, dummy.file, method, path, parameters)
