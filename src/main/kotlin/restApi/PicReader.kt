package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.Pic
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.util.pipeline.PipelineContext
import java.io.File

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Pic] is invalid.
 */
suspend fun PipelineContext<Unit, ApplicationCall>.readPic(): Pic? {
    var pic: Pic? = null
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val bytes = part.streamProvider().use { it.readBytes() }
                pic = try {
                    val type = Pic.Type.build(File(part.originalFileName!!).extension)
                    Pic(bytes, type)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    return pic
}