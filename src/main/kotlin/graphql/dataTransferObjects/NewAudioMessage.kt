package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class NewAudioMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription
