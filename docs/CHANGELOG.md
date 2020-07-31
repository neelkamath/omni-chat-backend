# Changelog

## v0.3.0

### GraphQL API

- New:
    - `MessageType` `enum`
- Updated:
    - `BareMessage` `interface`
    - `Message` `type`
    - `MessageData` `interface`
    - `StarredMessage` `type`
    - `NewMessage` `type`
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
    - `Subscription.subscribeToMessages` never sent back `MessageDeletionPoint`s.
    - `Subscription.subscribeToMessages` didn't send `UpdatedMessage`s to all subscribers.

## v0.1.0 (First Release)