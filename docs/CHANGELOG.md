# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The entire project (i.e., the GraphQL API, REST API, and server) uses the same version number. Major and minor versions get based off of the API (i.e., the GraphQL and REST APIs). For example, if the server has a backward incompatible change such as a DB schema update, but the APIs haven't changed, then only the patch number gets bumped. Another example is if the GraphQL API hasn't changed, but the format of the HTTP request used to send the GraphQL document has changed, then the major version gets bumped.

## [0.25.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.25.0) - 2021-07-17

### Fixed

- Fix the production `docker-compose.yml`.
- Fix the documentation for running the application in production.
- Fix the REST API docs.
- Fix `Mutation.makeGroupChatAdmins`.

## [0.24.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.24.0) - 2021-07-15

### Changed

- Rename the `pic-type` parameter to `image-type` in HTTP GET `/image-message`, HTTP GET `/profile-image`, and HTTP GET `/group-chat-image`.
- Rename `/pic-message` to `/image-message`.
- Rename `/profile-pic` to `/profile-image`.
- Rename `/group-chat-pic` to `/group-chat-image`.
- Rename `Mutation.deleteProfilePic` to `Mutation.deleteProfileImage`.
- Rename `Mutation.deleteGroupChatPic` to `Mutation.deleteGroupChatImage`.
- Rename `type UpdatedProfilePic` to `type UpdatedProfileImage`.
- Rename `type UpdatedGroupChatPic` to `type UpdatedGroupChatImage`.
- Rename `type PicMessage` to `type ImageMessage`.
- Rename `type BookmarkedPicMessage` to `type BookmarkedImageMessage`.
- Rename `type NewPicMessage` to `type NewImageMessage`.
- Remove authentication for HTTP GET `/group-chat-image`.

### Fixed

- Fix media downloads by sending back the send back the `Content-Disposition` header with the filename so that the browser downloads the file with the correct media type. The following operations have been fixed:
    - HTTP GET `/profile-image`
    - HTTP GET `/group-chat-image`
    - HTTP GET `/image-message`
    - HTTP GET `/audio-message`
    - HTTP GET `/video-message`
    - HTTP GET `/doc-message`

## [0.23.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.23.0) - 2021-07-04

### Changed

- Improve error handling for HTTP POST `/pic-message`, HTTP POST `/audio-message`, HTTP POST `/video-message`, and HTTP POST `/doc-message`.

    Previously, creating a message in a broadcast chat would cause an HTTP status code of 401 to be returned. Now, an HTTP status code of 400 is returned with the following `application/json` response body:

    ```json
    {
      "reason": "MUST_BE_ADMIN"
    }
    ```
- Add the field `chatId` to `interface BareGroupChat` and `type GroupChatInfo`.
- Rename `Query.readStars` to `Query.readBookmarks`.
- Rename `Mutation.star` to `Mutation.createBookmark`.
- Rename `Mutation.unstar` to `Mutation.deleteBookmark`.
- Rename `type UnstarredChat` to `type UnbookmarkedChat`.
- Rename `type StarredMessagesConnection` to `type BookmarkedMessagesConnection`.
- Rename `type StarredMessageEdge` to `type BookmarkedMessageEdge`.
- Rename the `hasStar` field to `isBookmarked` for the following:
    - `interface Message`
    - `type TextMessage`
    - `type ActionMessage`
    - `type PicMessage`
    - `type PollMessage`
    - `type AudioMessage`
    - `type GroupChatInviteMessage`
    - `type DocMessage`
    - `type VideoMessage`
    - `type UpdatedMessage`
- Rename `interface StarredMessage` to `interface BookmarkedMessage`.
- Rename `type StarredTextMessage` to `type BookmarkedTextMessage`.
- Rename `type StarredActionMessage` to `type BookmarkedActionMessage`.
- Rename `type StarredPicMessage` to `type BookmarkedPicMessage`.
- Rename `type StarredPollMessage` to `type BookmarkedPollMessage`.
- Rename `type StarredAudioMessage` to `type BookmarkedAudioMessage`.
- Rename `type StarredGroupChatInviteMessage` to `type BookmarkedGroupChatInviteMessage`.
- Rename `type StarredDocMessage` to `type BookmarkedDocMessage`.
- Rename `type StarredVideoMessage` to `type BookmarkedVideoMessage`.
- Change the size for media uploads and downloads from 5 MiB to 3 MiB for the following:
    - `/profile-pic`
    - `/group-chat-pic`
    - `/pic-message`
    - `/audio-message`
    - `/video-message`
    - `/doc-message`
- Allow creating an invitation for the chat itself using `Mutation.createGroupChatInviteMessage` instead of returning a `type InvalidChatId`.
- Allow forwarding a message in the chat it's from using `Mutation.forwardMessage` instead of returning a `type InvalidChatId`.
- Allow forwarding a group chat invite in the chat it's for using `Mutation.forwardMessage` instead of returning a `type InvalidChatId`.

### Fixed

- Fix `Mutation.leaveGroupChat` and `Mutation.removeGroupChatUsers`. They used to crash if every user left the chat when there were group chat invitations for the chat in other chats.
- Fix `Subscription.subscribeToMessages` not sending back `type UnbookmarkedChat`s.

## [0.22.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.22.0) - 2021-06-29

### Added

- `Query.readAllowedEmailAddressDomains`
- `union ReadAccountResult`

### Changed

- Change `type UpdatedPollMessage` to retrieve the entire poll instead for ease of use.
- Return a `union ReadAccountResult` instead of a `type Account` from `Query.readAccount`.
- Remove the `lastOnline` field from `type OnlineStatus`.
- Remove the `state` and `statuses` fields from the following:
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
    - `type UpdatedMessage`
- Remove the `state` field from the following:
    - `interface NewMessage`
    - `type NewTextMessage`
    - `type NewActionMessage`
    - `type NewPicMessage`
    - `type NewPollMessage`
    - `type NewAudioMessage`
    - `type NewGroupChatInviteMessage`
    - `type NewDocMessage`
    - `type NewVideoMessage`

### Removed

- `Mutation.createStatus`
- `type MessageDateTimeStatusConnection`
- `type MessageDateTimeStatusEdge`
- `type MessageDateTimeStatus`
- `enum MessageState`
- `enum MessageStatus`

### Fixed

- Fix `Mutation.deleteAccount` so that it doesn't crash when the user has messages in chats they're no longer in.
- Fix the `inviteCode` field for `type GroupChatInviteMessage`, `type NewGroupChatInviteMessage`, and `type StarredGroupChatInviteMessage`.

## [0.21.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.21.0) - 2021-06-16

### Added

- `union SearchGroupChatUsersResult`
- `Query.searchGroupChatUsers`
- `type UpdatedPollMessage`
- `union ReadMessageResult`
- `type MustBeAdmin`
- `type RemoveGroupChatUsersResult`
- `union SetPublicityResult`

### Changed

- Add `type MustBeAdmin` to the following:
    - `union CreatePollMessageResult`
    - `union CreateTextMessageResult`
    - `union ForwardMessageResult`
    - `union CreateGroupChatInviteMessageResult`
    - `union CreateActionMessageResult`
- Return a `type MustBeAdmin` in place of an HTTP status code of 401 from the following:
    - `Mutation.createPollMessage`
    - `Mutation.createTextMessage`
    - `Mutation.forwardMessage`
    - `Mutation.setBroadcast`
    - `Mutation.deleteGroupChatPic`
    - `Mutation.updateGroupChatDescription`
    - `Mutation.addGroupChatUsers`
    - `Mutation.updateGroupChatTitle`
    - `Mutation.makeGroupChatAdmins`
    - `Mutation.createGroupChatInviteMessage`
    - `Mutation.removeGroupChatUsers`
    - `Mutation.setPublicity`
    - `Mutation.createActionMessage`
- Return a `union ReadMessageResult` instead of a `interface Message` from `Query.readMessage`.
- Rename the `title` field to `question` in `type Poll` and `input PollInput`.
- Rename `Mutation.setInvitability` to `Mutation.setPublicity`.
- Add default values to `input GroupChatInput`.
- Update `Query.readAccount` to take the ID of the user to read, and stop requiring an access token.
- Make the `inviteCode` field of `type GroupChatInviteMessage`, `type StarredGroupChatInviteMessage`, and `type NewGroupChatInviteMessage` nullable.
- Send a `type UpdatedPollMessage` instead of a `type UpdatedMessage` when a user votes on a poll.
- Add `type UpdatedPollMessage` to `union MessagesSubscription` and `union ChatMessagesSubscription`.
- Return a `type InvalidChatId` instead of a `type InvalidInvitedChat` when `Mutation.createGroupChatInviteMessage` gets called with a chat the user isn't in.

### Removed

- Remove `type ExitedUsers` in favor of `type UpdatedGroupChat`'s `removedUsers` field.

### Fixed

- Make `type UpdatedGroupChat` return removed users in its `removedUsers` field.
- Allow reading a public chat the specified user isn't a participant of even if they send an access token to `Query.readChat` or `Query.searchChatMessages`.

## [0.20.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.20.0) - 2021-06-03

### Added

- `Subscription.subscribeToChatMessages`
- `Subscription.subscribeToChatOnlineStatuses`
- `Subscription.subscribeToChatTypingStatuses`
- `Subscription.subscribeToChatAccounts`
- `Subscription.subscribeToGroupChatMetadata`
- `type MessageDateTimeStatusConnection`
- `type MessageDateTimeStatusEdge`
- `type DeletedPrivateChat`

### Changed

- Update the following to paginate the `statuses` field:
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
    - `type UpdatedMessage`
- Rename `Subscription.subscribeToGroupChats` to `Subscription.subscribeToChats`.
- Rename `union GroupChatsSubscription` to `union ChatsSubscription`.
- Add `type DeletedPrivateChat` to `union ChatsSubscription`.

## [0.19.0](https://github.com/neelkamath/omni-chat-backend/releases/tag/v0.19.0) - 2021-05-21

### Added

- `Query.readMessage`

### Changed

- Return a `type InvalidChatId` from `Mutation.forwardMessage` when attempting to forward a message in the chat it's from instead of forwarding it.
- Change the field from `votes: [Int!]!` to `votes: [Account!]!` in `type PollOption`.
- Disallow using `Mutation.createGroupChatInviteMessage` by a user who isn't in the chat the invitation is for.
- Disallow using `Mutation.createGroupChatInviteMessage` when its `chatId` and `invitedChatId` arguments are the same.
- Disallow using `Mutations.forwardMessage` on group chat invite messages when its `chatId` argument is the same as the ID of the group chat the invitation is for.
- Rename Docker Hub image from `neelkamath/omni-chat` to `neelkamath/omni-chat-backend`.
- Make `Mutation.triggerAction` return a `Boolean!` instead of a `TriggerActionResult` for indicating whether the operation was successful.
- Yield the user's own typing statuses in `Subscription.subscribeToTypingStatuses`.
- Return the user's own ID in `Query.readTypingUsers`.
- Give `description` a default value of `""` in `input GroupChatInput`.
- Rename `NonexistentUser` to `NonexistingUser`.
- Rename `NonexistentOption` to `NonexistingOption`.
- Rename the `idList` argument to `userIdList` in `Mutation.addGroupChatUsers`.
- Rename the `idList` argument to `userIdList` in `Mutation.removeGroupChatUsers`.
- Rename the `idList` argument to `userIdList` in `Mutation.makeGroupChatAdmins`.
- Rename the `id` field to `chatId` in `type UnstarredChat`.
- Rename the `id` field to `userId` in `type DeletedAccount`.
- Rename the `id` field to `userId` in `type UpdatedProfilePic`.
- Rename the `id` field to `chatId` in `type UpdatedGroupChatPic`.
- Rename the `id` field to `chatId` in `type GroupChatId`.
- Rename the `id` field to `userId` in `type UpdatedAccount`.
- Rename the `id` field to `userId` in `interface AccountData`.
- Rename the `id` field to `userId` in `type Account`.
- Rename the `id` field to `userId` in `type BlockedAccount`.
- Rename the `id` field to `userId` in `type UnblockedAccount`.
- Rename the `id` field to `userId` in `type DeletedContact`.
- Rename the `id` field to `chatId` in `interface Chat`.
- Rename the `id` field to `chatId` in `type GroupChat`.
- Rename the `id` field to `chatId` in `type PrivateChat`.
- Rename the `id` field to `messageId` in `type MessageContext`.
- Rename the `id` field to `chatId` in `type CreatedChatId`.

### Fixed

- Fix pagination bugs.
- `Mutation.setOnline`
- `Mutation.joinGroupChat`
- `Mutation.triggerAction`
- `Mutation.leaveGroupChat`
- `Mutation.createActionMessage`
- `Query.readGroupChat`
- Stop yielding duplicate notifications from `Subscription.subscribeToTypingStatuses`.

### Removed

- `union TriggerActionResult`
- `type DeletionOfEveryMessage`
- `type MessageDeletionPoint`

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
