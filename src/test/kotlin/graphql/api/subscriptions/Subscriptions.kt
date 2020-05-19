package com.neelkamath.omniChat.test.graphql.api.subscriptions

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.jsonMapper
import com.neelkamath.omniChat.main
import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

typealias SubscriptionCallback = suspend (incoming: ReceiveChannel<Frame>, outgoing: SendChannel<Frame>) -> Unit

/**
 * Opens a WebSocket connection on the [uri], sends the GraphQL subscription [request], and has the [callback]
 * [ReceiveChannel] and [SendChannel].
 */
fun operateSubscription(
    uri: String,
    request: GraphQlRequest,
    accessToken: String,
    callback: SubscriptionCallback
): Unit = withTestApplication(Application::main) {
    handleWebSocketConversation(
        uri,
        { addHeader(HttpHeaders.Authorization, "Bearer $accessToken") }
    ) { receiveChannel, sendChannel ->
        launch(Dispatchers.IO) {
            val json = jsonMapper.writeValueAsString(request)
            sendChannel.send(Frame.Text(json))
        }.join()
        callback(receiveChannel, sendChannel)
    }
}

/** Waits until the [channel] sends a [Frame.Text], and returns its first [GraphQlResponse.errors] message. */
suspend fun parseFrameError(channel: ReceiveChannel<Frame>): String {
    for (frame in channel)
        if (frame is Frame.Text) {
            val text = frame.readText()
            return jsonMapper.readValue<GraphQlResponse>(text).errors!![0].message
        }
    throw Exception("There was no text frame to be read.")
}

/**
 * Returns the next [Frame.Text] (parsed as a [T]) so that you needn't deal with [Frame.Ping]s, etc.
 *
 * It is assumed that there was only one operation (i.e., that the GraphQL response's `"data"` key contains only one
 * key-value pair).
 */
suspend inline fun <reified T> parseFrameData(channel: ReceiveChannel<Frame>): T {
    for (frame in channel)
        if (frame is Frame.Text) {
            val response = jsonMapper.readValue<GraphQlResponse>(frame.readText()).data as Map<*, *>
            val data = response.values.first()!!
            return jsonMapper.convertValue(data)
        }
    throw Exception("There was no text frame to be read.")
}