# Omni Chat

_Trusted, Extensible, Better Chat_

![Cover](branding/facebook_cover_photo_2.png)

This repo is for the backend API. Here's a [web app](https://github.com/neelkamath/omni-chat-web) which utilizes the
API. Here are the features implemented so far:

- [x] Automatic online status. You don't manually set whether you're "away", or some other error-prone status that you
  have to constantly update, and no one takes seriously.
- [x] Private chats. These are for conversations between two people, like you and your friend.
- [x] See who's typing in the chat.
- [x] See when someone's online, or when they were last online.
- [x] Search the chat.
- [x] When a private chat gets deleted by a user, the messages sent until then are no longer visible to them, and the
  chat is no longer retrieved when requesting their chats. However, the user they were chatting with will still have the
  chat in the same state it was in before the user deleted it. If the other user sends a message to the user, it will
  show up as the first message in the user's chat.
- [x] Every message has the date and time it was sent, delivered, and read for each user.
- [x] Delete messages.
- [x] Star messages.
- [x] Markdown support.
- [x] Reply to a message to prevent context loss.
- [x] Block and unblock users without them knowing.
- Message types:
    - [x] Text
    - [x] Actions (i.e., buttons which trigger third-party server-side code such as ordering food via a bot)
    - [x] Audio
    - [x] Pictures
    - [x] Polls
    - [x] Videos
    - [x] Group chat invites
    - [x] Doc (i.e., any file type)
- Group chats (e.g., a school class's chat):
  - [x] Multiple admins.
  - [x] If you are added to a chat, or are added back to a chat after leaving it, you'll be able to see the entire chat'
    s history so that new participants don't miss older messages.
  - [x] If you leave a chat, your messages will remain in the chat until you delete your account, or join the chat again
    to delete them.
  - [x] Descriptions and icons.
  - [x] Unlimited participants.
  - [x] Broadcast chats (i.e., only admins can message). This option can be toggled for a chat any time. This is for
    chats for updates, like a conference's chat where you don't want hundreds of people asking the same questions over
    and over again.
  - [x] Group chat invite codes.

    This is useful for something like a college's elective class where hundreds of students from different sections need
    to be added. Instead of admins manually adding each of them, or manually adding one person from each section who in
    turn adds their classmates, the admin can simply auto-invite users via a code so that people will forward it to
    their relevant section's chat.

    This is how it'll work. Every chat gets associated with a UUID (Universally Unique IDentifier). Any user who enters
    this code gets added to the chat. The code isn't human readable so that hackers can't use brute force to join chats.
    Whether a chat can be joined via an invitation can be toggled by the admin; except for public chats where
    invitations are always on.
  - [x] Public chats (e.g., official Android chat, random groups individuals have created, Mario Kart chat). People can
    search for, and view public chats without an account. Anyone with an account can join them. A frontend UI may allow
    for a search engine to index the chat should the administrator allow for it.
- [x] Forward messages.
- [x] Omni Chat can be deployed for private use as well. For example, a company may only want to use it as an internal
  platform, in which case they can specify that only certain email address domains can create accounts. This way, even
  if an intruder gets into the company's network, they won't be able to create an account since they won't have a
  company issued email address. This feature also prevents employees from creating an account with their personal email
  address.
- [x] Bots can have buttons so that integrations can easily execute code. For example, if a Travis CI build fails, a bot
  could message the specifics on the group with a button, which when clicked, automatically reruns the CI/CD pipeline.
- [ ] Group audio calls.
- [ ] Group video calls.
- [ ] Screen sharing.
- [ ] Background noise cancellation for both audio and video calls.
- [ ] Spatial audio calls (important for gamers).

To view a previous version's docs, go to `https://github.com/neelkamath/omni-chat/tree/<VERSION>`, where `<VERSION>` is
the [release tag](https://github.com/neelkamath/omni-chat/tags) (e.g., `v0.1.1`).

Here are the guides for running the server using [Docker Compose](docs/docker-compose.md) (recommended for local
development), and the [cloud](docs/cloud.md) (recommended for production).

Here are [recommendations](docs/frontend-recommendations.md) if you're a developer creating a frontend UI utilizing this
backend API.

## Usage

- [Docs](docs/api.md)
  - Download the `rest-api.html` asset from a [release](https://github.com/neelkamath/omni-chat/releases). It'll be
    referenced in the docs.
  - Optionally, generate a wrapper for the GraphQL API
    using [GraphQL Code Generator](https://graphql-code-generator.com/)
    on [`schema.graphqls`](src/main/resources/schema.graphqls).
  - Optionally, generate a wrapper for the REST API using [OpenAPI Generator](https://openapi-generator.tech/)
    on [`openapi.yaml`](docs/openapi.yaml). Note that backwards compatible REST API updates don't guarantee backwards
    compatible wrappers. For example, a wrapper for REST API v0.3.1 may not be backwards compatible with a wrapper for
    REST API v0.3.0.
- [Changelog](docs/CHANGELOG.md)
- [Branding assets](branding)

## [Contributing](docs/CONTRIBUTING.md)

## Credits

[`dockerize`](docker/dockerize) was taken from [jwilder](https://github.com/jwilder/dockerize).

## License

This project is under the [MIT License](LICENSE).
