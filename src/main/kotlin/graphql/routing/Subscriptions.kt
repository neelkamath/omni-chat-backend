package com.neelkamath.omniChatBackend.graphql.routing

import com.auth0.jwt.exceptions.JWTVerificationException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.CreatedSubscription
import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.buildExecutionInput
import com.neelkamath.omniChatBackend.graphql.engine.buildSpecification
import com.neelkamath.omniChatBackend.graphql.engine.graphQl
import com.neelkamath.omniChatBackend.jwtVerifier
import com.neelkamath.omniChatBackend.objectMapper
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
    val completionReason: CloseReason,
)

/** Adds routes to the [context] which deal with GraphQL subscriptions. */
fun routeGraphQlSubscriptions(context: Routing) {
    val completionReason = CloseReason(CloseReason.Codes.NORMAL, "The user deleted their account.")
    val subscriptions = mapOf(
        "messages-subscription" to "subscribeToMessages",
        "chat-messages-subscription" to "subscribeToChatMessages",
        "accounts-subscription" to "subscribeToAccounts",
        "group-chats-subscription" to "subscribeToGroupChats",
        "typing-statuses-subscription" to "subscribeToTypingStatuses",
        "online-statuses-subscription" to "subscribeToOnlineStatuses",
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
 * couldn't be verified, or the account it belongs to has an unverified email address, the connection will be closed
 * with the [CloseReason.Codes.VIOLATED_POLICY]. If the subscription completes due to a server-side error, the
 * connection will be closed with the [CloseReason.Codes.INTERNAL_ERROR].
 */
private fun routeSubscription(context: Routing, path: String, subscription: GraphQlSubscription): Unit = with(context) {
    webSocket(path) {
        val unauthenticatedOperations = setOf("subscribeToChatMessages")
        val token = if (subscription.operation in unauthenticatedOperations) null else incoming.receive() as Frame.Text
        try {
            val result = buildExecutionResult(this, token?.readText())
            if (result.errors.isEmpty()) subscribe(this, subscription, result) else closeWithError(this, result)
        } catch (_: UnknownOperationException) {
            val reason = CloseReason(
                CloseReason.Codes.VIOLATED_POLICY,
                "You've sent multiple GraphQL operations, but haven't specified which one to execute.",
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
 * @throws UnknownOperationException
 * @throws JWTVerificationException if the [accessToken] wasn't `null`, and couldn't be verified.
 */
private suspend fun buildExecutionResult(
    session: DefaultWebSocketServerSession,
    accessToken: String?,
): ExecutionResult = with(session) {
    val userId = accessToken?.let { jwtVerifier.verify(it).subject.toInt() }
    // It's possible the user updated their email address just after the token was created.
    if (userId != null && !Users.hasVerifiedEmailAddress(userId))
        throw JWTVerificationException("The email address is unverified.")
    val frame = incoming.receive() as Frame.Text
    val request = objectMapper.readValue<GraphQlRequest>(frame.readText())
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
    result: ExecutionResult,
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
    private val graphQlSubscription: GraphQlSubscription,
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
                graphQlSubscription.operation to mapOf("__typename" to "CreatedSubscription", "placeholder" to ""),
            ),
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

    /** Closes the connection with a [CloseReason.Codes.INTERNAL_ERROR]. */
    override fun onError(throwable: Throwable): Unit = withSession {
        call.application.log.error(throwable)
        val reason = CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Error")
        close(reason)
    }

    /** Runs the [callback] in the [session]. */
    private inline fun withSession(
        crossinline callback: suspend DefaultWebSocketServerSession.() -> Unit,
    ): Unit = with(session) {
        launch(Dispatchers.IO) { callback() }
    }

    /** [Subscription.cancel]s the [Subscription]. */
    fun cancel(): Unit = subscription.cancel()
}
