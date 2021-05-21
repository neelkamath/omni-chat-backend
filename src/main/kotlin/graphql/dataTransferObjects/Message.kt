@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.MessageType
import com.neelkamath.omniChatBackend.db.tables.MessageStatuses
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.Stargazers
import com.neelkamath.omniChatBackend.graphql.routing.MessageState
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDateTime

interface Message {
    /** The [Messages.id]. */
    val id: Int

    fun getMessageId(): Int = id

    fun getSender(): Account = Account(Messages.readSenderId(id))

    fun getState(): MessageState = Messages.readState(id)

    fun getSent(): LocalDateTime = Messages.readSent(id)

    fun getStatuses(): List<MessageDateTimeStatus> = MessageStatuses.readIdList(id).map(::MessageDateTimeStatus)

    fun getContext(): MessageContext = MessageContext(id)

    fun getIsForwarded(): Boolean = Messages.isForwarded(id)

    fun getHasStar(env: DataFetchingEnvironment): Boolean {
        val userId = env.userId
        return if (userId == null) false else Stargazers.hasStar(userId, this.id)
    }

    companion object {
        /**
         * Returns one of [TextMessage], [ActionMessage], [AudioMessage], [DocMessage], [GroupChatInviteMessage],
         * [PicMessage], [PollMessage], and [VideoMessage] based on the [messageId]'s type.
         */
        fun build(messageId: Int): Message = when (Messages.readType(messageId)) {
            MessageType.TEXT -> TextMessage(messageId)
            MessageType.ACTION -> ActionMessage(messageId)
            MessageType.AUDIO -> AudioMessage(messageId)
            MessageType.DOC -> DocMessage(messageId)
            MessageType.GROUP_CHAT_INVITE -> GroupChatInviteMessage(messageId)
            MessageType.PIC -> PicMessage(messageId)
            MessageType.POLL -> PollMessage(messageId)
            MessageType.VIDEO -> VideoMessage(messageId)
        }
    }
}
