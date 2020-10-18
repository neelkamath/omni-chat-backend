package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.create
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

private fun postVideoMessage(
    accessToken: String,
    dummy: DummyFile,
    chatId: Int,
    contextMessageId: Int? = null
): TestApplicationResponse {
    val parameters = listOf("chat-id" to chatId.toString(), "context-message-id" to contextMessageId?.toString())
        .filter { it.second != null }
        .formUrlEncode()
    return uploadFile(accessToken, dummy, HttpMethod.Post, "video-message", parameters)
}

@ExtendWith(DbExtension::class)
class VideoMessageTest {
    @Nested
    inner class RouteVideoMessage {
        @Test
        fun `Using a capitalized file extension shouldn't fail`() {
            val admin = createVerifiedUsers(1)[0]
            val chatId = GroupChats.create(listOf(admin.info.id))
            val dummy = DummyFile("video.MP4", bytes = 1)
            assertEquals(HttpStatusCode.NoContent, postVideoMessage(admin.accessToken, dummy, chatId).status())
        }
    }
}
