# Changelog

## v0.2.0

### Backward Incompatible Changes

- The `Bio`, `GroupChatDescription`, and `TextMessage` `scalar`s now use [CommonMark](https://commonmark.org) instead of plain text.

## v0.1.1

### New Features

- `Query.readStars`
- `Mutation.star`
- `Mutation.deleteStar`
- A `hasStar` field has been added to the `Message` and `UpdatedMessage` `type`s. 

### Bug Fixes

- `Subscription.subscribeToMessages` never sent back `MessageDeletionPoint`s.
- `Subscription.subscribeToMessages` didn't send `UpdatedMessage`s to all subscribers.

## v0.1.0

- First release.