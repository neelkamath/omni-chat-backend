package com.neelkamath.omniChat.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.objectMapper
import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

typealias SubscriptionCallback = suspend (incoming: ReceiveChannel<Frame>) -> Unit

/**
 * Opens a WebSocket connection on the [uri], sends the GraphQL subscription [request], and has the [callback]
 * [ReceiveChannel] and [SendChannel].
 */
fun executeGraphQlSubscriptionViaWebSocket(
    uri: String,
    request: GraphQlRequest,
    accessToken: String? = null,
    callback: SubscriptionCallback
): Unit = withTestApplication(Application::main) {
    handleWebSocketConversation(
        uri,
        { if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken") }
    ) { incoming, outgoing ->
        launch(Dispatchers.IO) {
            val json = objectMapper.writeValueAsString(request)
            outgoing.send(Frame.Text(json))
        }.join()
        callback(incoming)
    }
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
            val response = objectMapper.readValue<GraphQlResponse>(frame.readText()).data as Map<*, *>
            return objectMapper.convertValue(response.values.first()!!)
        }
    throw Exception("There was no text frame to be read.")
}