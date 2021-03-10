# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The entire project (i.e., the GraphQL API, REST API, and server) uses the same version number. Major and minor versions get based off of the API (i.e., the GraphQL and REST APIs). For example, if the server has a backward incompatible change such as a DB schema update, but the APIs haven't changed, then only the patch number gets bumped. Another example is if the GraphQL API hasn't changed, but the format of the HTTP request used to send the GraphQL document has changed, then the major version gets bumped.

## 0.17.0

### Fixed

- Fix `Mutation.deleteAccount`.

## [0.16.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.16.0) - 2021-03-10

### Added

- `Query.readTypingStatuses`
- `UpdatedProfilePic` `type`
- `UpdatedGroupChatPic` `type`

### Changed

- `Mutation.blockUser`
- `Mutation.unblockUser`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `Mutation.deleteContacts`
- `Mutation.createContacts`
- Send back an `ExitedUser` over `Subscription.subscribeToGroupChats` if the user themselves left the chat.
- `UpdatedAccount` `type`
- `UpdatedGroupChat` `type`
- `UpdatedOnlineStatus` `type`
- `UpdatedMessage` `interface`
- `UpdatedTextMessage` `type`
- `UpdatedActionMessage` `type`
- `UpdatedPicMessage` `type`
- `UpdatedPollMessage` `type`
- `UpdatedAudioMessage` `type`
- `UpdatedGroupChatInviteMessage` `type`
- `UpdatedDocMessage` `type`
- `UpdatedVideoMessage` `type`
- `TextMessage` `type`
- `ActionMessage` `type`
- `NewTextMessage` `type`
- `NewActionMessage` `type`
- `StarredTextMessage` `type`
- `StarredActionMessage` `type`
- `GroupChatsSubscription` `union`
- `AccountsSubscription` `union`
- Return the messages from `Query.searchChatMessages` in chronological order.
- Return the messages from `Query.searchMessages` in chronological order.

### Removed

- `Query.isBlocked`
- `Query.isContact`

### Fixed

- Fix some notifications not being sent back to clients over WebSockets.
- Fix `NewActionMessage` `type`, etc. not being supported in `Subscription`s.
- `Query.searchChatMessages` works now.
- `Query.searchMessages` works now.

## [0.15.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.15.0) - 2021-02-15

### Added

- `Mutation.setOnline`
- `Mutation.setBroadcast`

### Changed

- `Mutation.createStatus`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `Mutation.createGroupChat`
- `Mutation.createContacts`

### Removed

- `Mutation.setOnlineStatus`
- `Mutation.setBroadcastStatus`
- `QueryIsUsernameTaken`
- `Query.isEmailAddressTaken`
- `Query.canDeleteAccount`

### Fixed

- `Mutation.triggerAction`

## [0.14.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.14.0) - 2021-02-09

### Fixed

- `ActionMessage` `type`
- `StarredActionMessage` `type`
- `NewActionMessage` `type`
- `UpdatedActionMessage` `type`
- `/pic-message`
- `/audio-message`
- `/video-message`
- `/doc-message`
- `/group-chat-pic`

## [0.13.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.13.0) - 2021-01-31

### Changed

- `Mutation.createPrivateChat`

## [0.12.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.12.0) - 2021-01-30

### Changed

- `BlockedAccount` `type`
- `UnblockedAccount` `type`

## [0.11.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.11.0) - 2021-01-29

### Fixed

- Send back the `__typename` for the `CreatedSubscription` `type` for `Subscription`s.
- Send back the `BlockedAccount` and `UnblockedAccount` `type`s in `Subscription.subscribeToAccounts`.

## [0.10.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.10.0) - 2021-01-29

### Added

- `Query.isBlocked`
- `Query.isContact`

## [0.9.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.9.0) - 2021-01-27

### Added

- `Query.readBlockedUsers`
- `Mutation.blockUser`
- `Mutation.unblockUser`
- `BlockedAccount` `type`
- `UnblockedAccount` `type`

### Changed

- `AccountsSubscription` `union`
- `/profile-pic`
- `/group-chat-pic`
- `/pic-message`
- `/audio-message`
- `/video-message`
- `/doc-message`

## [0.8.3](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.8.3) - 2021-01-10

### Fixed

- Fix HTTP PATCH requests.

## [0.8.2](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.8.2) - 2020-12-31

### Fixed

- Fix memory leaks.

## [0.8.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.8.1) - 2020-10-26

### Changed

- Rename `DB_PASSWORD` to `POSTGRES_PASSWORD`.

### Fixed

- Fix `docker-compose.yml`.

## [0.8.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.8.0) - 2020-10-18

### Changed

- Docker `message-broker` service's `image`
- Rename the `ALLOWED_DOMAINS` environment variable to `ALLOWED_EMAIL_DOMAINS`.
- Handle MP4 audio on the `/audio-message` endpoint.

### Removed

- Docker `proxy` service
- `Query.requestOnetimeToken`

### Fixed

- Return an error when `Mutation.emailEmailAddressVerification` is supplied a verified email address.
- Fix file uploads with capital letters in the file extension.

### Security

- Send access tokens as a text event instead of in the URL for `Subscription`s.

## [0.7.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.7.1) - 2020-09-13

### Fixed

- The Docker image no longer requires authentication to pull from the registry.

## [0.7.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.7.0) - 2020-09-13

### Added

- Docker `db` service
- `Name` `scalar`
- `Mutation.verifyEmailAddress`
- `Mutation.emailPasswordResetCode`

### Changed

- `.env` file
- `Mutation.updateAccount`
- Rename `Mutation.sendEmailAddressVerification` to `Mutation.emailEmailAddressVerification`
- `UpdatedAccount` `type`
- `AccountData` `interface`
- `Account` `type`
- `NewContact` `type`
- `AccountInput` `input`
- `AccountUpdate` `input`

### Removed

- Docker `auth` service
- Docker `auth-db` service
- Docker `chat-db` service

## [0.6.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.6.1) - 2020-09-04

### Added

- `Mutation.createActionMessage`
- `Mutation.triggerAction`
- `TriggeredAction` `type`
- `ActionMessageInput` `input`
- `ActionableMessage` `type`
- `ActionMessage` `type`
- `StarredActionMessage` `type`
- `NewActionMessage` `type`
- `UpdatedActionMessage` `type`

### Changed

- `Query.searchChatMessages`
- `Query.searchMessages`
- `Mutation.setPollVote`
- `MessagesSubscription` `union`

### Fixed

- Fix the example Docker Compose file for Windows users.
- Don't give back `null` `errors` keys in GraphQL documents send back over `Subscription`s.

## [0.6.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.6.0) - 2020-08-31

### Changed

- Docker `auth` service's `image`
- Docker `auth-db` service's `image`
- Docker `chat-db` service's `image`

### Fixed

- `ALLOWED_EMAIL_DOMAINS` environment variable

### Security

- Access tokens are now to be sent in the URL instead of an HTTP header for WebSockets.

## [0.5.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.5.0) - 2020-08-15

### Added

- `Subscription.subscribeToAccounts`
- `Subscription.subscribeToGroupChats`
- `AccountsSubscription` `union`
- `GroupChatsSubscription` `union`
- `GroupChatPublicity` `enum`

### Changed

- `Mutation.createAccount`
- `UpdatedOnlineStatus` `type`
- `BareGroupChat` `interface`
- `GroupChatInfo` `type`
- `GroupChat` `type`
- `GroupChatInput` `input`

### Removed

- `Subscription.subscribeToContacts`
- `Subscription.subscribeToUpdatedChats`
- `Subscription.subscribeToNewGroupChats`
- `ContactsSubscription` `union`
- `UpdatedChatsSubscription` `union`
- `NewGroupChatsSubscription` `union`
- `UpdatedContact` `type`

### Fixed

- `Query.readOnlineStatuses`
- `Subscription.subscribeToOnlineStatuses`

## [0.4.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.4.0) - 2020-08-13

### Added

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
- `/video-message`
- `/doc-message`

### Changed

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
- `/group-chat-pic`

### Fixed

- `Mutation.deleteGroupChatPic`
- `Mutation.updateGroupChatTitle`
- `Mutation.updateGroupChatDescription`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `Mutation.setBroadcastStatus`

## [0.3.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.3.1) - 2020-08-08

### Fixed

- `Subscription`

## [0.3.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.3.0) - 2020-08-03

### Added

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
- `/audio-message`
- `/pic-message`

### Changed

- `MessagesSubscription` `union`
- `Query.searchChatMessages`
- `Query.searchMessages`
- `BareMessage` `interface`
- `/profile-pic`
- `/group-chat-pic`

### Removed

- `Mutation.createMessage`
- `MessageData` `interface`
- `NewMessage` `type`
- `StarredMessage` `type`
- `TextMessage` `scalar`
- `Message` `type`
- `UpdatedMessage` `type`

## [0.2.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.2.1) - 2020-07-26

### Added

- `Mutation.setBroadcastStatus`

### Changed

- `Mutation.createMessage`
- `UpdatedGroupChat` `type`
- `GroupChat` `type`
- `GroupChatInput` `input`

## [0.2.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.2.0) - 2020-07-25

### Added

- `Mutation.updateGroupChatTitle`
- `Mutation.updateGroupChatDescription`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `AccountInput` `input`
- `GroupChatInput` `input`

### Changed

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
- `/profile-pic`
- `/group-chat-pic`

### Removed

- `Mutation.leaveGroupChat`
- `Mutation.updateGroupChat`
- `NewAccount` `input`
- `NewGroupChat` `input`

## [0.1.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.1.1) - 2020-07-21

### Added

- `Query.readStars`
- `Mutation.star`
- `Mutation.deleteStar`

### Changed

- `Message` `type`
- `UpdatedMessage` `type`

### Fixed

- `Subscription.subscribeToMessages`
- `Subscription.subscribeToMessages`

## [0.1.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.1.0) - 2020-07-19

### Added

- First version.
