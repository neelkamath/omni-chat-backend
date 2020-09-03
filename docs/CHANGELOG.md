# Changelog

## v0.6.1

### Server

- Bug fixes:
    - The example Docker Compose file now works for Windows users.

### GraphQL API

- Bug fixes:
    - `Subscription`s no longer give back GraphQL documents with `null` `errors` keys.

## v0.6.0

### Server

- Updated:
    - Docker `auth` service's `image`
    - Docker `auth-db` service's `image`
    - Docker `chat-db` service's `image`
- Bug fixes:
    - `ALLOWED_DOMAINS` environment variable
    - Access tokens are now to be sent in the URL instead of an HTTP header for WebSockets.

## v0.5.0

### GraphQL QPI

- New:
    - `Subscription.subscribeToAccounts`
    - `Subscription.subscribeToGroupChats`
    - `AccountsSubscription` `union`
    - `GroupChatsSubscription` `union`
    - `GroupChatPublicity` `enum`
- Updated:
    - `Mutation.createAccount`
    - `UpdatedOnlineStatus` `type`
    - `BareGroupChat` `interface`
    - `GroupChatInfo` `type`
    - `GroupChat` `type`
    - `GroupChatInput` `input`
- Removed:
    - `Subscription.subscribeToContacts`
    - `Subscription.subscribeToUpdatedChats`
    - `Subscription.subscribeToNewGroupChats`
    - `ContactsSubscription` `union`
    - `UpdatedChatsSubscription` `union`
    - `NewGroupChatsSubscription` `union`
    - `UpdatedContact` `type`
- Bug fixes:
    - `Query.readOnlineStatuses`
    - `Subscription.subscribeToOnlineStatuses`

## v0.4.0

### GraphQL API

- New:
    - `Uuid` `scalar`
    - `VideoMessage` `type`
    - `StarredVideoMessage` `type`
    - `NewVideoMessage` `type`
    - `UpdatedVideoMessage` `type`
    - `DocMessage` `type`
    - `StarredDocMessage` `type`
    - `NewDocMessage` `type`
    - `UpdatedDocMessage` `type`
    - `Query.readGroupChat`
    - `Mutation.joinGroupChat`
    - `Mutation.createGroupChatInviteMessage`
    - `GroupChatInfo` `type`
    - `BareGroupChat` `interface`
    - `GroupChatInviteMessage` `type`
    - `StarredGroupChatInviteMessage` `type`
    - `NewGroupChatInviteMessage` `type`
    - `UpdatedGroupChatInviteMessage` `type`
    - `Query.searchPublicChats`
    - `Mutation.setInvitability`
    - `Mutation.forwardMessage`
- Updated:
    - `TextMessage` `type`
    - `PicMessage` `type`
    - `PollMessage` `type`
    - `AudioMessage` `type`
    - `UpdatedGroupChat` `type`
    - `GroupChat` `type`
    - `StarredTextMessage` `type`
    - `StarredPicMessage` `type`
    - `StarredPollMessage` `type`
    - `StarredAudioMessage` `type`
    - `NewMessage` `interface`
    - `NewTextMessage` `type`
    - `NewPicMessage` `type`
    - `NewPollMessage` `type`
    - `NewAudioMessage` `type`
    - `UpdatedMessage` `interface`
    - `UpdatedTextMessage` `type`
    - `UpdatedPicMessage` `type`
    - `UpdatedPollMessage` `type`
    - `UpdatedAudioMessage` `type`
    - `GroupChatInput` `input`
    - `MessagesSubscription` `union`
    - `BareMessage` `interface`
    - `BareChatMessage` `interface`
    - `StarredMessage` `interface`
    - `Message` `interface`
    - `Query.searchChatMessages`
    - `Query.readChat`
- Bug fixes:
    - `Mutation.deleteGroupChatPic`
    - `Mutation.updateGroupChatTitle`
    - `Mutation.updateGroupChatDescription`
    - `Mutation.addGroupChatUsers`
    - `Mutation.removeGroupChatUsers`
    - `Mutation.makeGroupChatAdmins`
    - `Mutation.setBroadcastStatus`

### REST API

- New:
    - `/video-message`
    - `/doc-message`
- Updated:
    - `/group-chat-pic`

## v0.3.1

### GraphQL API
- Bug fixes:
    - `Subscription`

## v0.3.0

### GraphQL API

- New:
    - `Mutation.createTextMessage`
    - `Mutation.createPollMessage`
    - `Mutation.setPollVote`
    - `Message` `interface`
    - `MessageText` `scalar`
    - `PollInput` `input`
    - `PollOption` `type`
    - `Poll` `type`
    - `TextMessage` `type`
    - `PicMessage` `type`
    - `PollMessage` `type`
    - `AudioMessage` `type`
    - `BareChatMessage` `interface`
    - `StarredMessage` `interface`
    - `StarredTextMessage` `type`
    - `StarredPicMessage` `type`
    - `StarredPollMessage` `type`
    - `StarredAudioMessage` `type`
    - `NewMessage` `interface`
    - `NewTextMessage` `type`
    - `NewPicMessage` `type`
    - `NewPollMessage` `type`
    - `NewAudioMessage` `type`
    - `UpdatedMessage` `interface`
    - `UpdatedTextMessage` `type`
    - `UpdatedPicMessage` `type`
    - `UpdatedPollMessage` `type`
    - `UpdatedAudioMessage` `type`
- Updated:
    - `MessagesSubscription` `union`
    - `Query.searchChatMessages`
    - `Query.searchMessages`
    - `BareMessage` `interface`
- Removed:
    - `Mutation.createMessage`
    - `MessageData` `interface`
    - `NewMessage` `type`
    - `StarredMessage` `type`
    - `TextMessage` `scalar`
    - `Message` `type`
    - `UpdatedMessage` `type`

### REST API

- New:
    - `/audio-message`
    - `/pic-message`
- Updated:
    - `/profile-pic`
    - `/group-chat-pic`

## v0.2.1

### GraphQL API

- New:
    - `Mutation.setBroadcastStatus`
- Updated:
    - `Mutation.createMessage`
    - `UpdatedGroupChat` `type`
    - `GroupChat` `type`
    - `GroupChatInput` `input`

## v0.2.0

### GraphQL API

- New:
    - `Mutation.updateGroupChatTitle`
    - `Mutation.updateGroupChatDescription`
    - `Mutation.addGroupChatUsers`
    - `Mutation.removeGroupChatUsers`
    - `Mutation.makeGroupChatAdmins`
    - `AccountInput` `input`
    - `GroupChatInput` `input`
- Updated:
    - `Mutation.createGroupChat`
    - `Mutation.createMessage`
    - `BareMessage` `interface`
    - `Message` `type`
    - `MessageData` `interface`
    - `StarredMessage` `type`
    - `NewMessage` `type`
    - `UpdatedMessage` `type`
    - `UpdatedGroupChat` `type`
    - `GroupChat` `type`
    - `GroupChatUpdate` `input`
    - `GroupChatInput` `input`
    - `NewGroupChatsSubscription` `union`
    - `Bio` `scalar`
    - `GroupChatDescription` `scalar`
    - `TextMessage` `scalar`
- Removed:
    - `Mutation.leaveGroupChat`
    - `Mutation.updateGroupChat`
    - `NewAccount` `input`
    - `NewGroupChat` `input`

### REST API

- Updated:
    - `/profile-pic`
    - `/group-chat-pic`

## v0.1.1

### GraphQL API

- New:
    - `Query.readStars`
    - `Mutation.star`
    - `Mutation.deleteStar`
- Updated:
    - `Message` `type`
    - `UpdatedMessage` `type`
- Bug fixes:
    - `Subscription.subscribeToMessages`
    - `Subscription.subscribeToMessages`

## v0.1.0 (First Release)