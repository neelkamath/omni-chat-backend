package com.neelkamath.omniChat.graphql.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.testingObjectMapper

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

typealias SubscriptionCallback = suspend (incoming: ReceiveChannel<Frame>) -> Unit

/**
 * Opens a WebSocket connection on the URI's [path] (e.g., `"messages-subscription"`), sends the GraphQL subscription
 * [request], and has the [callback] [ReceiveChannel] and [SendChannel].
 */
fun executeGraphQlSubscriptionViaWebSocket(
    path: String,
    request: GraphQlRequest,
    accessToken: String? = null,
    callback: SubscriptionCallback
): Unit = withTestApplication(Application::main) {
    handleWebSocketConversation(path) { incoming, outgoing ->
        if (accessToken != null) outgoing.send(Frame.Text(accessToken))
        launch(Dispatchers.IO) {
            val json = testingObjectMapper.writeValueAsString(request)
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
            val response = testingObjectMapper.readValue<GraphQlResponse>(frame.readText()).data as Map<*, *>
            return testingObjectMapper.convertValue(response.values.first()!!)
        }
    throw Exception("There was no text frame to be read.")
}
