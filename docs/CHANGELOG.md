# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The entire project (i.e., the GraphQL API, REST API, and server) uses the same version number. Major and minor versions get based off of the API (i.e., the GraphQL and REST APIs). For example, if the server has a backward incompatible change such as a DB schema update, but the APIs haven't changed, then only the patch number gets bumped. Another example is if the GraphQL API hasn't changed, but the format of the HTTP request used to send the GraphQL document has changed, then the major version gets bumped.

## 0.19.0

### Changed

- Rename Docker image from `neelkamath/omni-chat` to `neelkamath/omni-chat-backend`.

### Fixed

- `Mutation.setOnline`
- Fix pagination bugs.

## [0.18.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.18.0) - 2021-04-06

### Added

- `Query.searchBlockedUsers`
- `type DeletedAccount`
- `Mutation.createContact`
- `Mutation.deleteContact`
- `type StarredMessagesConnection`
- `type StarredMessageEdge`
- `type ChatMessagesConnection`
- `type ChatMessagesEdge`
- `type ChatsConnection`
- `type ChatEdge`
- `type GroupChatsConnection`
- `type GroupChatEdge`

### Changed

- Rename `chatId` to `id` in `type UnstarredChat`.
- `scalar Username`
- Add `type DeletedAccount` to `union AccountsSubscription`.
- Return a `Boolean!` instead of a `Placeholder!` from `Mutation.unblockUser` to indicate whether the user got unblocked.
- Use GitHub Flavored Markdown instead of CommonMark (update `scalar Bio`, `scalar GroupChatDescription`, and `scalar MessageText` accordingly).
- Paginate `Query.readStars`.
- Paginate `Query.searchMessages`.
- Paginate `Query.readChats`.
- Paginate `Query.searchChats`.
- Paginate `Query.searchPublicChats`.
- Remove the `dateTimes` field, and add the `sent` field to the following:
    - `interface NewMessage`
    - `type NewTextMessage`
    - `type NewActionMessage`
    - `type NewPicMessage`
    - `type NewPollMessage`
    - `type NewAudioMessage`
    - `type NewGroupChatInviteMessage`
    - `type NewDocMessage`
    - `type NewVideoMessage`
- Remove the `dateTimes` field, and add the `sent` and `statuses` fields to the following:
    - `interface Message`
    - `type TextMessage`
    - `type ActionMessage`
    - `type PicMessage`
    - `type PollMessage`
    - `type AudioMessage`
    - `type GroupChatInviteMessage`
    - `type DocMessage`
    - `type VideoMessage`
    - `interface StarredMessage`
    - `type StarredTextMessage`
    - `type StarredActionMessage`
    - `type StarredPicMessage`
    - `type StarredPollMessage`
    - `type StarredAudioMessage`
    - `type StarredGroupChatInviteMessage`
    - `type StarredDocMessage`
    - `type StarredVideoMessage`

### Removed

- Remove `Mutation.createContacts` in favor of `Mutation.createContact`.
- Remove `Mutation.deleteContacts` in favor of `Mutation.deleteContact`.
- `interface BareMessage`
- `interface BareChatMessage`
- `type MessageDateTimes`

### Fixed

- Fix pagination bugs.

## [0.17.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.17.0) - 2021-03-19

### Added

- `enum MessageState`
- `type TypingUsers`
- `type NonexistentUser`
- `type UnverifiedEmailAddress`
- `type EmailAddressVerified`
- `type UsernameTaken`
- `type IncorrectPassword`
- `type EmailAddressTaken`
- `type InvalidChatId`
- `type InvalidAdminId`
- `type UnregisteredEmailAddress`
- `type InvalidUserId`
- `type InvalidMessageId`
- `type CannotDeleteAccount`
- `type InvalidPoll`
- `type NonexistentOption`
- `type InvalidInviteCode`
- `type InvalidInvitedChat`
- `type InvalidDomain`
- `type InvalidAction`
- `type MessageEdges`
- `type InvalidVerificationCode`
- `type InvalidPasswordResetCode`
- `type CreatedChatId`
- `union SearchChatMessagesResult`
- `union ReadChatResult`
- `union ReadGroupChatResult`
- `union RequestTokenSetResult`
- `union VerifyEmailAddressResult`
- `union ResetPasswordResult`
- `union UpdateAccountResult`
- `union CreateAccountResult`
- `union EmailEmailAddressVerificationResult`
- `union CreateGroupChatResult`
- `union CreatePrivateChatResult`
- `union CreateTextMessageResult`
- `union CreateActionMessageResult`
- `union CreateGroupChatInviteMessageResult`
- `union CreatePollMessageResult`
- `union ForwardMessageResult`
- `union TriggerActionResult`
- `union SetPollVoteResult`
- `union LeaveGroupChatResult`
- `union ReadOnlineStatusResult`
- `type UnstarredChat`
- `type CannotLeaveChat`
- `Query.readOnlineStatus`
- `Mutation.joinPublicChat`
- `Mutation.leaveGroupChat`
- `type UpdatedMessage`

### Changed

- Rename `Mutation.deleteStar` to `Mutation.unstar`.
- Add the field `state: MessageState!` to the following:
    - `interface BareMessage`
    - `interface Message`
    - `type TextMessage`
    - `type ActionMessage`
    - `type PicMessage`
    - `type PollMessage`
    - `type AudioMessage`
    - `type GroupChatInviteMessage`
    - `type DocMessage`
    - `type VideoMessage`
    - `interface BareChatMessage`
    - `interface StarredMessage`
    - `type StarredTextMessage`
    - `type StarredActionMessage`
    - `type StarredPicMessage`
    - `type StarredPollMessage`
    - `type StarredAudioMessage`
    - `type StarredGroupChatInviteMessage`
    - `type StarredDocMessage`
    - `type StarredVideoMessage`
    - `interface NewMessage`
    - `type NewTextMessage`
    - `type NewActionMessage`
    - `type NewPicMessage`
    - `type NewPollMessage`
    - `type NewAudioMessage`
    - `type NewGroupChatInviteMessage`
    - `type NewDocMessage`
    - `type NewVideoMessage`
- GraphQL operations used to return results related to invalid input in the GraphQL document's `errors[0].message`. Such results are supposed to be returned in the GraphQL document's `data` value instead. Change the following operations' return types accordingly:
    - `Query.searchChatMessages`
    - `Query.readChat`
    - `Query.readGroupChat`
    - `Query.requestTokenSet`
    - `Mutation.blockUser`
    - `Mutation.deleteAccount`
    - `Mutation.verifyEmailAddress`
    - `Mutation.resetPassword`
    - `Mutation.star`
    - `Mutation.setTyping`
    - `Mutation.createStatus`
    - `Mutation.updateAccount`
    - `Mutation.createAccount`
    - `Mutation.emailEmailAddressVerification`
    - `Mutation.emailPasswordResetCode`
    - `Mutation.removeGroupChatUsers`
    - `Mutation.createGroupChat`
    - `Mutation.setInvitability`
    - `Mutation.joinGroupChat`
    - `Mutation.deletePrivateChat`
    - `Mutation.createPrivateChat`
    - `Mutation.createTextMessage`
    - `Mutation.createActionMessage`
    - `Mutation.createGroupChatInviteMessage`
    - `Mutation.createPollMessage`
    - `Mutation.forwardMessage`
    - `Mutation.triggerAction`
    - `Mutation.setPollVote`
    - `Mutation.deleteMessage`
- Disallow leading and trailing whitespace in the following `scalar`s:
    - `Bio`
    - `GroupChatTitle`
    - `GroupChatDescription`
    - `MessageText`
- Add the following to `union MessagesSubscription`:
    - `type UnstarredChat`
    - `type UpdatedMessage`
- Remove the following from `union MessagesSubscription`:
    - `type UpdatedTextMessage`
    - `type UpdatedActionMessage`
    - `type UpdatedPicMessage`
    - `type UpdatedAudioMessage`
    - `type UpdatedGroupChatInviteMessage`
    - `type UpdatedDocMessage`
    - `type UpdatedVideoMessage`
    - `type UpdatedPollMessage`
    - `type UnstarredChat`
- Replace `type ExitedUser` with `type ExitedUsers`.
- Replace `type UpdatedOnlineStatus` with `type OnlineStatus` in `union OnlineStatusesSubscription`.
- Replace `Query.readTypingStatuses` with `Query.readTypingUsers`.

### Fixed

- `Mutation.deleteAccount`
- Unstar any messages the user starred in the chat they've left when calling `Mutation.deletePrivateChat`, `Mutation.leaveGroupChat`, or `Mutation.removeGroupChatUsers`.

### Removed

- Remove `type UpdatedOnlineStatus` in favor of `type OnlineStatus`.
- Remove `Query.readOnlineStatuses` in favor of `Query.readOnlineStatus`.
- Remove the following in favor of `type UpdatedMessage`:
    - `interface UpdatedMessage`
    - `type UpdatedTextMessage`
    - `type UpdatedAudioMessage`
    - `type UpdatedPicMessage`
    - `type UpdatedPollMessage`
    - `type UpdatedVideoMessage`
    - `type UpdatedActionMessage`
    - `type UpdatedDocMessage`
    - `type UpdatedGroupChatInviteMessage`

## [0.16.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.16.0) - 2021-03-10

### Added

- `Query.readTypingStatuses`
- `type UpdatedProfilePic`
- `type UpdatedGroupChatPic`

### Changed

- `Mutation.blockUser`
- `Mutation.unblockUser`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `Mutation.deleteContacts`
- `Mutation.createContacts`
- Send back an `ExitedUser` over `Subscription.subscribeToGroupChats` if the user themselves left the chat.
- `type UpdatedAccount`
- `type UpdatedGroupChat`
- `type UpdatedOnlineStatus`
- `interface UpdatedMessage`
- `type UpdatedTextMessage`
- `type UpdatedActionMessage`
- `type UpdatedPicMessage`
- `type UpdatedPollMessage`
- `type UpdatedAudioMessage`
- `type UpdatedGroupChatInviteMessage`
- `type UpdatedDocMessage`
- `type UpdatedVideoMessage`
- `type TextMessage`
- `type ActionMessage`
- `type NewTextMessage`
- `type NewActionMessage`
- `type StarredTextMessage`
- `type StarredActionMessage`
- `union GroupChatsSubscription`
- `union AccountsSubscription`
- Return the messages from `Query.searchChatMessages` in chronological order.
- Return the messages from `Query.searchMessages` in chronological order.

### Removed

- `Query.isBlocked`
- `Query.isContact`

### Fixed

- Fix some notifications not being sent back to clients over WebSockets.
- Fix `type NewActionMessage`, etc. not being supported in `Subscription`s.
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

- `type ActionMessage`
- `type StarredActionMessage`
- `type NewActionMessage`
- `type UpdatedActionMessage`
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

- `type BlockedAccount`
- `type UnblockedAccount`

## [0.11.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.11.0) - 2021-01-29

### Fixed

- Send back the `__typename` for the `type CreatedSubscription` for `Subscription`s.
- Send back the `BlockedAccount` and `type UnblockedAccount`s in `Subscription.subscribeToAccounts`.

## [0.10.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.10.0) - 2021-01-29

### Added

- `Query.isBlocked`
- `Query.isContact`

## [0.9.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.9.0) - 2021-01-27

### Added

- `Query.readBlockedUsers`
- `Mutation.blockUser`
- `Mutation.unblockUser`
- `type BlockedAccount`
- `type UnblockedAccount`

### Changed

- `union AccountsSubscription`
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
- `scalar Name`
- `Mutation.verifyEmailAddress`
- `Mutation.emailPasswordResetCode`

### Changed

- `.env` file
- `Mutation.updateAccount`
- Rename `Mutation.sendEmailAddressVerification` to `Mutation.emailEmailAddressVerification`
- `type UpdatedAccount`
- `interface AccountData`
- `type Account`
- `type NewContact`
- `input AccountInput`
- `input AccountUpdate`

### Removed

- Docker `auth` service
- Docker `auth-db` service
- Docker `chat-db` service

## [0.6.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.6.1) - 2020-09-04

### Added

- `Mutation.createActionMessage`
- `Mutation.triggerAction`
- `type TriggeredAction`
- `input ActionMessageInput`
- `type ActionableMessage`
- `type ActionMessage`
- `type StarredActionMessage`
- `type NewActionMessage`
- `type UpdatedActionMessage`

### Changed

- `Query.searchChatMessages`
- `Query.searchMessages`
- `Mutation.setPollVote`
- `union MessagesSubscription`

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
- `union AccountsSubscription`
- `union GroupChatsSubscription`
- `enum GroupChatPublicity`

### Changed

- `Mutation.createAccount`
- `type UpdatedOnlineStatus`
- `interface BareGroupChat`
- `type GroupChatInfo`
- `type GroupChat`
- `input GroupChatInput`

### Removed

- `Subscription.subscribeToContacts`
- `Subscription.subscribeToUpdatedChats`
- `Subscription.subscribeToNewGroupChats`
- `union ContactsSubscription`
- `union UpdatedChatsSubscription`
- `union NewGroupChatsSubscription`
- `type UpdatedContact`

### Fixed

- `Query.readOnlineStatuses`
- `Subscription.subscribeToOnlineStatuses`

## [0.4.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.4.0) - 2020-08-13

### Added

- `scalar Uuid`
- `type VideoMessage`
- `type StarredVideoMessage`
- `type NewVideoMessage`
- `type UpdatedVideoMessage`
- `type DocMessage`
- `type StarredDocMessage`
- `type NewDocMessage`
- `type UpdatedDocMessage`
- `Query.readGroupChat`
- `Mutation.joinGroupChat`
- `Mutation.createGroupChatInviteMessage`
- `type GroupChatInfo`
- `interface BareGroupChat`
- `type GroupChatInviteMessage`
- `type StarredGroupChatInviteMessage`
- `type NewGroupChatInviteMessage`
- `type UpdatedGroupChatInviteMessage`
- `Query.searchPublicChats`
- `Mutation.setInvitability`
- `Mutation.forwardMessage`
- `/video-message`
- `/doc-message`

### Changed

- `type TextMessage`
- `type PicMessage`
- `type PollMessage`
- `type AudioMessage`
- `type UpdatedGroupChat`
- `type GroupChat`
- `type StarredTextMessage`
- `type StarredPicMessage`
- `type StarredPollMessage`
- `type StarredAudioMessage`
- `interface NewMessage`
- `type NewTextMessage`
- `type NewPicMessage`
- `type NewPollMessage`
- `type NewAudioMessage`
- `interface UpdatedMessage`
- `type UpdatedTextMessage`
- `type UpdatedPicMessage`
- `type UpdatedPollMessage`
- `type UpdatedAudioMessage`
- `input GroupChatInput`
- `union MessagesSubscription`
- `interface BareMessage`
- `interface BareChatMessage`
- `interface StarredMessage`
- `interface Message`
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
- `interface Message`
- `scalar MessageText`
- `input PollInput`
- `type PollOption`
- `type Poll`
- `type TextMessage`
- `type PicMessage`
- `type PollMessage`
- `type AudioMessage`
- `interface BareChatMessage`
- `interface StarredMessage`
- `type StarredTextMessage`
- `type StarredPicMessage`
- `type StarredPollMessage`
- `type StarredAudioMessage`
- `interface NewMessage`
- `type NewTextMessage`
- `type NewPicMessage`
- `type NewPollMessage`
- `type NewAudioMessage`
- `interface UpdatedMessage`
- `type UpdatedTextMessage`
- `type UpdatedPicMessage`
- `type UpdatedPollMessage`
- `type UpdatedAudioMessage`
- `/audio-message`
- `/pic-message`

### Changed

- `union MessagesSubscription`
- `Query.searchChatMessages`
- `Query.searchMessages`
- `interface BareMessage`
- `/profile-pic`
- `/group-chat-pic`

### Removed

- `Mutation.createMessage`
- `interface MessageData`
- `type NewMessage`
- `type StarredMessage`
- `scalar TextMessage`
- `type Message`
- `type UpdatedMessage`

## [0.2.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.2.1) - 2020-07-26

### Added

- `Mutation.setBroadcastStatus`

### Changed

- `Mutation.createMessage`
- `type UpdatedGroupChat`
- `type GroupChat`
- `input GroupChatInput`

## [0.2.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.2.0) - 2020-07-25

### Added

- `Mutation.updateGroupChatTitle`
- `Mutation.updateGroupChatDescription`
- `Mutation.addGroupChatUsers`
- `Mutation.removeGroupChatUsers`
- `Mutation.makeGroupChatAdmins`
- `input AccountInput`
- `input GroupChatInput`

### Changed

- `Mutation.createGroupChat`
- `Mutation.createMessage`
- `interface BareMessage`
- `type Message`
- `interface MessageData`
- `type StarredMessage`
- `type NewMessage`
- `type UpdatedMessage`
- `type UpdatedGroupChat`
- `type GroupChat`
- `input GroupChatUpdate`
- `input GroupChatInput`
- `union NewGroupChatsSubscription`
- `scalar Bio`
- `scalar GroupChatDescription`
- `scalar TextMessage`
- `/profile-pic`
- `/group-chat-pic`

### Removed

- `Mutation.leaveGroupChat`
- `Mutation.updateGroupChat`
- `input NewAccount`
- `input NewGroupChat`

## [0.1.1](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.1.1) - 2020-07-21

### Added

- `Query.readStars`
- `Mutation.star`
- `Mutation.deleteStar`

### Changed

- `type Message`
- `type UpdatedMessage`

### Fixed

- `Subscription.subscribeToMessages`
- `Subscription.subscribeToMessages`

## [0.1.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.1.0) - 2020-07-19

### Added

- First version.
