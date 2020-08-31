package com.neelkamath.omniChat.graphql.routing

import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.db.tables.OnetimeTokens
import com.neelkamath.omniChat.graphql.engine.buildExecutionInput
import com.neelkamath.omniChat.graphql.engine.buildSpecification
import com.neelkamath.omniChat.graphql.engine.graphQl
import com.neelkamath.omniChat.isInvalidOnetimeToken
import com.neelkamath.omniChat.jwtVerifier
import com.neelkamath.omniChat.objectMapper
import graphql.ExecutionResult
import graphql.execution.UnknownOperationException
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

private data class GraphQlSubscription(
    /** Operation name (e.g., `"subscribeToMessages"`). */
    val operation: String,
    /** The [CloseReason] to send the client when the subscription is successfully completed. */
    val completionReason: CloseReason
)

/** Adds routes to the [context] which deal with GraphQL subscriptions. */
fun routeGraphQlSubscriptions(context: Routing) {
    val completionReason = CloseReason(CloseReason.Codes.NORMAL, "The user deleted their account.")
    val subscriptions = mapOf(
        "messages-subscription" to "subscribeToMessages",
        "accounts-subscription" to "subscribeToAccounts",
        "group-chats-subscription" to "subscribeToGroupChats",
        "typing-statuses-subscription" to "subscribeToTypingStatuses",
        "online-statuses-subscription" to "subscribeToOnlineStatuses"
    )
    for ((path, operation) in subscriptions)
        routeSubscription(context, path, GraphQlSubscription(operation, completionReason))
}

/**
 * Binds a WebSocket for the GraphQL [subscription] at the [path] (e.g., `"messages-subscription"`).
 *
 * Once subscribed, the client will receive a [CreatedSubscription].
 *
 * If the client sends an invalid [GraphQlRequest.query], the [GraphQlResponse.errors] will be sent back, and then the
 * connection will be closed with the [CloseReason.Codes.VIOLATED_POLICY]. If an access token was received but it
 * couldn't be verified, or it's an already used onetime token, the connection will be closed with the
 * [CloseReason.Codes.VIOLATED_POLICY]. If the subscription completes due to a server-side error, the connection will be
 * closed with the [CloseReason.Codes.INTERNAL_ERROR].
 */
private fun routeSubscription(context: Routing, path: String, subscription: GraphQlSubscription): Unit = with(context) {
    webSocket(path) {
        try {
            val result = buildExecutionResult(this, call.parameters["access_token"])
            if (result.errors.isEmpty()) subscribe(this, subscription, result) else closeWithError(this, result)
        } catch (_: UnknownOperationException) {
            val reason = CloseReason(
                CloseReason.Codes.VIOLATED_POLICY,
                "You've sent multiple GraphQL operations, but haven't specified which one to execute."
            )
            send(Frame.Close(reason))
        } catch (_: JWTVerificationException) {
            val reason = CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized")
            send(Frame.Close(reason))
        }
    }
}

/**
 * Parses the [DefaultWebSocketServerSession.incoming] [Frame.Text] as a [GraphQlRequest], and executes it.
 *
 * @throws [UnknownOperationException]
 * @throws [JWTVerificationException] if the [accessToken] couldn't be verified, or it's an already used onetime token.
 */
private suspend fun buildExecutionResult(
    session: DefaultWebSocketServerSession,
    accessToken: String?
): ExecutionResult = with(session) {
    val frame = incoming.receive() as Frame.Text
    val request = objectMapper.readValue<GraphQlRequest>(frame.readText())
    val userId = accessToken?.let { token ->
        val jwt = jwtVerifier.verify(token)
        if (isInvalidOnetimeToken(jwt)) throw JWTVerificationException("This onetime token has already been used.")
        jwt.id?.let { OnetimeTokens.delete(it.toInt()) }
        jwt.subject.toInt()
    }
    val builder = buildExecutionInput(request, userId)
    return graphQl.execute(builder)
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
) = with(session) {
    val subscriber = Subscriber(this, subscription)
    result.getData<Publisher<ExecutionResult>>().subscribe(subscriber)
    closeReason.await()
    subscriber.cancel()
}

/**
 * [DefaultWebSocketServerSession.send]s the [ExecutionResult.toSpecification], and then closes the connection with the
 * [CloseReason.Codes.VIOLATED_POLICY] and the first [GraphQlResponse.errors] [GraphQlResponseError.message].
 */
private suspend fun closeWithError(session: DefaultWebSocketServerSession, result: ExecutionResult): Unit =
    with(session) {
        val spec = try {
            buildSpecification(result)
        } catch (_: UnauthorizedException) {
            val reason = CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized")
            session.send(Frame.Close(reason))
            return
        }
        val response = objectMapper.convertValue<GraphQlResponse>(spec)
        launch(Dispatchers.IO) {
            val json = objectMapper.writeValueAsString(response)
            send(Frame.Text(json))
        }.join()
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
@Suppress("ReactiveStreamsSubscriberImplementation")
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
        val doc = mapOf(
            "data" to mapOf(
                graphQlSubscription.operation to mapOf(
                    "placeholder" to ""
                )
            )
        )
        val json = objectMapper.writeValueAsString(doc)
        withSession { send(Frame.Text(json)) }
    }

    /**
     * [DefaultWebSocketServerSession.send]s the [ExecutionResult.toSpecification], and makes a [Subscription.request].
     */
    override fun onNext(result: ExecutionResult) {
        sendResult(result)
        subscription.request(1)
    }

    private fun sendResult(result: ExecutionResult) {
        val json = objectMapper.writeValueAsString(buildSpecification(result))
        withSession { send(Frame.Text(json)) }
    }

    /** Closes the connection with the [GraphQlSubscription.completionReason]. */
    override fun onComplete(): Unit = withSession { close(graphQlSubscription.completionReason) }

    /** Closes the connection using [CloseReason.Codes.INTERNAL_ERROR]. */
    override fun onError(throwable: Throwable): Unit = withSession {
        call.application.log.error(throwable)
        val reason = CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error")
        close(reason)
    }

    /** Runs the [callback] in the [session]. */
    private inline fun withSession(
        crossinline callback: suspend DefaultWebSocketServerSession.() -> Unit
    ): Unit = with(session) {
        launch(Dispatchers.IO) { callback() }
    }

    /** [Subscription.cancel]s the [Subscription]. */
    fun cancel(): Unit = subscription.cancel()
}