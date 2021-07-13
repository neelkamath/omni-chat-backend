package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import java.io.File
import java.util.UUID.randomUUID
import javax.annotation.processing.Generated

fun buildFile(filename: Filename, bytes: ByteArray): File =
    File.createTempFile("$filename-${randomUUID()}", File(filename).extension).apply { writeBytes(bytes) }

/** The [bytes] are the file's contents. */
data class TypedFile(val filename: Filename, val bytes: ByteArray) {
    /** The file's extension (excluding the dot), or an empty [String] if it doesn't have one. */
    val extension = File(filename).extension

    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypedFile

        if (filename != other.filename) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class MediaFile(val filename: Filename, val bytes: ByteArray) {
    @Generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaFile

        if (filename != other.filename) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    @Generated
    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * The `imageType` argument passed to the [bytesReader] will be `null` if no `image-type` query parameter was passed.
 */
inline fun getMediaMessage(
    route: Route,
    crossinline bytesReader: (messageId: Int, imageType: ImageType?) -> MediaFile,
): Unit = with(route) {
    get {
        val messageId = call.parameters["message-id"]!!.toInt()
        val imageType = call.parameters["image-type"]?.let(ImageType::valueOf)
        if (Messages.isVisible(call.userId, messageId)) {
            val (filename, bytes) = bytesReader(messageId, imageType)
            call.respondFile(buildFile(filename, bytes))
        } else call.respond(HttpStatusCode.Unauthorized)
    }
}

fun <T> postMediaMessage(
    route: Route,
    messageReader: suspend PipelineContext<Unit, ApplicationCall>.() -> T?,
    creator: (userId: Int, chatId: Int, message: T, contextMessageId: Int?) -> Unit,
): Unit = with(route) {
    post {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val contextMessageId = call.parameters["context-message-id"]?.toInt()
        val message = messageReader(this)
        when {
            !isUserInChat(call.userId!!, chatId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.USER_NOT_IN_CHAT),
            )

            message == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_FILE))

            !Messages.isValidContext(call.userId!!, chatId, contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidMediaMessage(InvalidMediaMessage.Reason.INVALID_CONTEXT_MESSAGE),
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidMediaMessage(InvalidMediaMessage.Reason.MUST_BE_ADMIN))

            else -> {
                creator(call.userId!!, chatId, message, contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [VideoFile] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartMp4(): VideoFile? {
    val file = readMultipartFile()
    if (file.extension.lowercase() != "mp4" || file.bytes.size > VideoFile.MAX_BYTES) return null
    return VideoFile(file.filename, file.bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [DocFile] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartDoc(): DocFile? {
    val (filename, bytes) = readMultipartFile()
    return if (bytes.size > DocFile.MAX_BYTES) null else DocFile(filename, bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [AudioFile] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartAudio(): AudioFile? {
    val file = readMultipartFile()
    if (!AudioFile.isValidExtension(file.extension) || file.bytes.size > AudioFile.MAX_BYTES) return null
    return AudioFile(file.filename, file.bytes)
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [ProcessedImage] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartImage(): ProcessedImage? {
    val (filename, bytes) = readMultipartFile()
    return try {
        ProcessedImage.build(filename, bytes)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Receives a multipart request with only one part, where the part is a [PartData.FileItem]. */
private suspend fun PipelineContext<Unit, ApplicationCall>.readMultipartFile(): TypedFile {
    val part = call.receiveMultipart().readPart()!! as PartData.FileItem
    val filename = part.originalFileName!!
    val bytes = part.streamProvider().use { it.readBytes() }
    part.dispose()
    return TypedFile(filename, bytes)
}
