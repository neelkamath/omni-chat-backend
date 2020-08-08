# Changelog

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