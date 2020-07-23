# Changelog

## v0.2.0

### GraphQL API

- Backwards compatible changes:
    - `Mutation.createMessage` takes an optional `contextMessageId` parameter to allow for replying to specific messages.
    - A `context` field has been added to the `BareMessage` `interface`, `Message` `type`, `MessageData` `interface`, `StarredMessage` `type`, `NewMessage` `type`, and `UpdatedMessage` `type`.
- Breaking changes:
    - The `Bio`, `GroupChatDescription`, and `TextMessage` `scalar`s now use [CommonMark](https://commonmark.org) instead of plain text.
    - The `NewAccount` `type` has been renamed to `AccountInput`.
    - The `NewGroupChat` `type` has been replaced by `GroupChatInput`.

### REST API

- Breaking changes:
    - Images must be PNGs or JPEGs not exceeding 100 KiB.

## v0.1.1

### GraphQL API

- Backwards compatible changes:
    - `Query.readStars`
    - `Mutation.star`
    - `Mutation.deleteStar`
    - A `hasStar` field has been added to the `Message` and `UpdatedMessage` `type`s. 
- Bug fixes:
    - `Subscription.subscribeToMessages` never sent back `MessageDeletionPoint`s.
    - `Subscription.subscribeToMessages` didn't send `UpdatedMessage`s to all subscribers.

## v0.1.0

- First release.