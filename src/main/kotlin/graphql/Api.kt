package com.neelkamath.omniChat.graphql

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import graphql.ExecutionResult
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.util.error
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.webSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

data class GraphQlSubscription(
    /** Operation name (e.g., `"messageUpdates"`). */
    val operation: String,
    /** The [CloseReason] to send the client when the subscription is successfully completed. */
    val completionReason: CloseReason
)

fun routeGraphQl(context: Routing): Unit = with(context) {
    authenticate(optional = true) {
        post("graphql") {
            val builder = buildExecutionInput(call.receive(), call)
            val result = graphQl.execute(builder)
            call.respond(buildSpecification(result))
        }
    }
}

/**
 * Binds a WebSocket for the GraphQL [subscription] at the [path] (e.g., `"message-updates"`).
 *
 * Once subscribed, the client will receive a [CreatedSubscription].
 *
 * If the client sends an invalid [GraphQlRequest.query], the [GraphQlResponse.errors] will be sent back, and then the
 * connection will be closed with the [CloseReason.Codes.VIOLATED_POLICY]. If the subscription completes due to a
 * server-side error, the connection will be closed with the [CloseReason.Codes.INTERNAL_ERROR].
 */
fun routeSubscription(context: Routing, path: String, subscription: GraphQlSubscription): Unit = with(context) {
    authenticate {
        webSocket(path) {
            val result = buildExecutionResult(this)
            if (result.errors.isEmpty()) subscribe(this, subscription, result) else onError(this, result)
        }
    }
}

/**
 * Sends the client the [result]'s events. Once subscribed, the client will receive a [CreatedSubscription].
 *
 * If the subscription completes due to a server-side error, the connection will be closed with the
 * [CloseReason.Codes.INTERNAL_ERROR].
 */
private suspend fun subscribe(
    session: DefaultWebSocketServerSession,
    subscription: GraphQlSubscription,
    result: ExecutionResult
): Unit = with(session) {
    val subscriber = Subscriber(this, subscription)
    result.getData<Publisher<ExecutionResult>>().subscribe(subscriber)
    closeReason.await()
    subscriber.cancel()
}

/** Parses the [DefaultWebSocketServerSession.incoming] [Frame.Text] as a [GraphQlRequest], and executes it. */
private suspend fun buildExecutionResult(session: DefaultWebSocketServerSession): ExecutionResult = with(session) {
    val frame = incoming.receive() as Frame.Text
    val text = frame.readText()
    val request = objectMapper.readValue<GraphQlRequest>(text)
    val builder = buildExecutionInput(request, call)
    return graphQl.execute(builder)
}

/**
 * [DefaultWebSocketServerSession.send]s the [ExecutionResult.toSpecification], and then closes the connection with the
 * [CloseReason.Codes.VIOLATED_POLICY] and the first [GraphQlResponse.errors] [GraphQlResponseError.message].
 */
private suspend fun onError(session: DefaultWebSocketServerSession, result: ExecutionResult): Unit = with(session) {
    val spec = buildSpecification(result)
    val response = objectMapper.convertValue<GraphQlResponse>(spec)
    sendResponse(this, response)
    onErrorClose(this, response)
}

/** [DefaultWebSocketServerSession.send]s the [response]. */
private suspend fun sendResponse(session: DefaultWebSocketServerSession, response: GraphQlResponse): Unit =
    with(session) {
        launch(Dispatchers.IO) {
            val json = objectMapper.writeValueAsString(response)
            send(Frame.Text(json))
        }.join()
    }

/**
 * Closes the [DefaultWebSocketServerSession] with the [CloseReason.Codes.VIOLATED_POLICY] and the first
 * [GraphQlResponse.errors] [GraphQlResponseError.message].
 */
private suspend fun onErrorClose(session: DefaultWebSocketServerSession, response: GraphQlResponse): Unit =
    with(session) {
        val message = response.errors!![0].message
        val reason = CloseReason(CloseReason.Codes.VIOLATED_POLICY, message)
        send(Frame.Close(reason))
    }

/**
 * [DefaultWebSocketServerSession.send]s the [graphQlSubscription]'s data to the client.
 *
 * Once subscribed, the client will receive a [CreatedSubscription]. If the subscription completes due to a server-side
 * error, the connection will be closed with the [CloseReason.Codes.INTERNAL_ERROR].
 */
private class Subscriber(
    private val session: DefaultWebSocketServerSession,
    private val graphQlSubscription: GraphQlSubscription
) : Subscriber<ExecutionResult> {

    private lateinit var subscription: Subscription

    /** [DefaultWebSocketServerSession.send]s a [CreatedSubscription], and makes a [Subscription.request]. */
    override fun onSubscribe(subscription: Subscription) {
        this.subscription = subscription
        sendCreatedSubscription()
        subscription.request(1)
    }

    /** [DefaultWebSocketServerSession.send]s a [CreatedSubscription]. */
    private fun sendCreatedSubscription() {
        val data = mapOf(graphQlSubscription.operation to CreatedSubscription())
        val response = GraphQlResponse(data)
        val json = objectMapper.writeValueAsString(response)
        runInSessionContext { send(Frame.Text(json)) }
    }

    /**
     * [DefaultWebSocketServerSession.send]s the [ExecutionResult.toSpecification], and makes a [Subscription.request].
     */
    override fun onNext(result: ExecutionResult) {
        sendResult(result)
        subscription.request(1)
    }

    private fun sendResult(result: ExecutionResult) {
        val spec = buildSpecification(result)
        val json = objectMapper.writeValueAsString(spec)
        runInSessionContext { send(Frame.Text(json)) }
    }

    /** Closes the connection with the [GraphQlSubscription.completionReason]. */
    override fun onComplete(): Unit = runInSessionContext { close(graphQlSubscription.completionReason) }

    /** Closes the connection using [CloseReason.Codes.INTERNAL_ERROR]. */
    override fun onError(throwable: Throwable): Unit = runInSessionContext {
        call.application.log.error(throwable)
        val reason = CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error")
        close(reason)
    }

    /** Runs the [callback] in the [session]. */
    private inline fun runInSessionContext(
        crossinline callback: suspend DefaultWebSocketServerSession.() -> Unit
    ): Unit = with(session) {
        launch(Dispatchers.IO) { callback() }
    }

    /** [Subscription.cancel]s the [Subscription]. */
    fun cancel(): Unit = subscription.cancel()
}