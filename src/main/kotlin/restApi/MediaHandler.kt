package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.Mp3
import com.neelkamath.omniChat.db.tables.Mp4
import com.neelkamath.omniChat.userId
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.pipeline.PipelineContext
import java.io.File
import javax.annotation.processing.Generated

data class MultipartFile(val extension: String, val bytes: ByteArray) {
    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MultipartFile

        if (extension != other.extension) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = extension.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

inline fun getMediaMessage(route: Route, crossinline bytesReader: (messageId: Int) -> ByteArray): Unit = with(route) {
    get {
        val messageId = call.parameters["message-id"]!!.toInt()
        if (Messages.isVisible(call.userId!!, messageId)) call.respondBytes(bytesReader(messageId))
        else call.respond(HttpStatusCode.BadRequest)
    }
}

fun <T> postMediaMessage(
    route: Route,
    messageReader: suspend PipelineContext<Unit, ApplicationCall>.() -> T?,
    creator: (userId: Int, chatId: Int, message: T, contextMessageId: Int?) -> Unit
): Unit = with(route) {
    post {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val contextMessageId = call.parameters["context-message-id"]?.toInt()
        val message = messageReader(this)
        when {
            !isUserInChat(call.userId!!, chatId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT)
            )

            message == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE))

            contextMessageId != null && !Messages.exists(contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE)
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)

            else -> {
                creator(call.userId!!, chatId, message, contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Mp4] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartMp4(): Mp4? {
    val (extension, bytes) = readMultipartFile()
    return if (extension != "mp4" || bytes.size > Mp4.MAX_BYTES) null else Mp4(bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Mp3] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartMp3(): Mp3? {
    val (extension, bytes) = readMultipartFile()
    return if (extension != "mp3" || bytes.size > Mp3.MAX_BYTES) null else Mp3(bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Pic] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartPic(): Pic? {
    val (extension, bytes) = readMultipartFile()
    return try {
        Pic(bytes, Pic.Type.build(extension))
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Receives a multipart request with only one part, where the part is a [PartData.FileItem]. */
private suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartFile(): MultipartFile {
    var multipartFile: MultipartFile? = null
    call.receiveMultipart().forEachPart { part ->
        multipartFile = when (part) {
            is PartData.FileItem ->
                MultipartFile(File(part.originalFileName!!).extension, part.streamProvider().use { it.readBytes() })
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    return multipartFile!!
}