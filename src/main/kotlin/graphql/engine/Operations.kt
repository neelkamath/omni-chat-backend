package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.graphql.operations.*
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.TypeRuntimeWiring

/** Wires the GraphQL queries, mutations, and subscriptions to the [builder]. */
fun wireGraphQlOperations(builder: RuntimeWiring.Builder): RuntimeWiring.Builder =
    builder.type("Query", ::wireQuery).type("Mutation", ::wireMutation).type("Subscription", ::wireSubscription)

private fun wireQuery(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("canDeleteAccount", ::canDeleteAccount)
    .dataFetcher("readAccount", ::readAccount)
    .dataFetcher("isUsernameTaken", ::isUsernameTaken)
    .dataFetcher("isEmailAddressTaken", ::isEmailAddressTaken)
    .dataFetcher("readChat", ::readChat)
    .dataFetcher("readChats", ::readChats)
    .dataFetcher("searchChats", ::searchChats)
    .dataFetcher("readContacts", ::readContacts)
    .dataFetcher("searchContacts", ::searchContacts)
    .dataFetcher("searchMessages", ::searchMessages)
    .dataFetcher("requestTokenSet", ::requestTokenSet)
    .dataFetcher("refreshTokenSet", ::refreshTokenSet)
    .dataFetcher("searchChatMessages", ::searchChatMessages)
    .dataFetcher("searchUsers", ::searchUsers)

private fun wireMutation(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("deleteAccount", ::deleteAccount)
    .dataFetcher("createAccount", ::createAccount)
    .dataFetcher("setOnlineStatus", ::setOnlineStatus)
    .dataFetcher("updateAccount", ::updateAccount)
    .dataFetcher("deleteProfilePic", ::deleteProfilePic)
    .dataFetcher("deleteGroupChatPic", ::deleteGroupChatPic)
    .dataFetcher("sendEmailAddressVerification", ::sendEmailAddressVerification)
    .dataFetcher("resetPassword", ::resetPassword)
    .dataFetcher("leaveGroupChat", ::leaveGroupChat)
    .dataFetcher("updateGroupChat", ::updateGroupChat)
    .dataFetcher("createStatus", ::createStatus)
    .dataFetcher("createGroupChat", ::createGroupChat)
    .dataFetcher("setTyping", ::setTyping)
    .dataFetcher("deletePrivateChat", ::deletePrivateChat)
    .dataFetcher("createPrivateChat", ::createPrivateChat)
    .dataFetcher("createMessage", ::createMessage)
    .dataFetcher("deleteContacts", ::deleteContacts)
    .dataFetcher("createContacts", ::createContacts)
    .dataFetcher("deleteMessage", ::deleteMessage)

private fun wireSubscription(builder: TypeRuntimeWiring.Builder): TypeRuntimeWiring.Builder = builder
    .dataFetcher("subscribeToMessages", ::subscribeToMessages)
    .dataFetcher("subscribeToContacts", ::subscribeToContacts)
    .dataFetcher("subscribeToUpdatedChats", ::subscribeToUpdatedChats)
    .dataFetcher("subscribeToNewGroupChats", ::subscribeToNewGroupChats)
    .dataFetcher("subscribeToTypingStatuses", ::subscribeToTypingStatuses)