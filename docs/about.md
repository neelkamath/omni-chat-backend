# About

## The Problem

Currently, we must use a combination of WhatsApp, Slack, Telegram, etc. to communicate. This causes the following issues:

- Each time a topic’s information is needed, multiple apps must be searched to piece together the information part by part.
- There’s a mental burden of switching between apps during the same conversation. For example, you may be texting your coworkers on Slack but then need to switch to a Google Meet call to screen share.
- Switching apps causes noticeable delays which takes away the main point of instant messaging.
- Switching between apps results in businesses losing productivity.
- Having to maintain multiple accounts annoys the average user because none of the apps fulfill the user’s needs well enough.

## User Stories

### Overachiever Otis

Overachiever Otis has signed up for Google Summer of Code, a program where he works with open source project maintainers to develop quality software during his summer break. Before he can put the rest of his classmates to shame, he must first pick an organization to work with.

Overachiever Otis first applies to an organization which creates databases. He sees the mentor’s email address listed, and shoots him an email. The mentor replies that he must contact him through Gitter instead. When he sends the same query via Gitter, he’s told to discuss the topic on the project’s forum.

Overachiever Otis then applied to a GNU project. Though the mentor communicates with him via email, he must use their issue tracker to discuss actual project ideas, and their IRC channel for help on specific topics. GNU only allows the usage of free software, so when the mentor wants to jump on a call, he’s told to use Mumble.

This ordeal leaves Overachiever Otis frustrated. He wishes he didn’t have to set up so many apps just to send a few messages.

### Gamer Glenda

Gamer Glenda uses Discord while gaming for group audio calls on public game servers. She uses WhatsApp while she’s on the go to catch up on family and college chats. When she’s at home, she usually uses Facebook Messenger on her laptop. She uses Slack to message her coworkers at the startup she’s interning at. She uses Instagram to stay in touch with her friends from the UK where she used to live as a kid, and Snapchat for her old classmates.

Gamer Glenda’s friends and family are on a combination of these apps, and they contact her on different apps at different times. Her friends text her on both WhatsApp and Discord depending on which app they happen to be using at the time. Since it’s impractical to be ~~boring~~ professional the entire day, occasionally her coworkers will goof off on Slack during work hours, and send work related messages on WhatsApp after-hours. All of this causes the same conversations to be scattered around sans context.

Gamer Glenda wishes she didn’t have to use so many apps so that her and others’ communications would be well-received.

### Startup Shamu

Startup Shamu is the CEO of his latest startup, and has decided to use Skype for internal communications since it has text, audio, and video chat. It also keeps the video chat’s messages in the chat’s history so they don’t get lost after a meeting.

As Startup Shamu’s company grows, they switch to Slack since it’s easier to use, they prefer the way Slack handles sensitive data, and it comes with many integrations for tasks such as notifying software developers when their application has crashed.

Though Slack works well for many things, it’s call system isn’t up to the mark. For basic audio calls, employees use the built-in Slack calls feature but sometimes have to switch to Google Meet when they need to share their screen. Since Google Meet only has video chat, messages sent in it are lost afterwards.

Startup Shamu has many problems related to internal communications. He has to pay Zoom’s fees, worry about third party companies leaking their sensitive data, and his employees are less productive since they have to switch between multiple communication channels throughout the day to discuss the same topic. He wishes there was a communications app he could self-host so he wouldn’t need to worry about these issues while still being able to integrate with third party APIs.

### Vain Vanessa

Vain Vanessa is a single mom who owns a small business which sells organic and natural beauty products. She wants to have a chat embedded on her store’s website to help customers so that she can improve sales. She doesn’t want to hire a software developer to create a brand new chat app, and she doesn’t want to use one of the existing web chat app integrations because that’ll require her to set up, and use another communications channel.

Her requirements are that a chatbot would initially greet the customer, and answer FAQs. If the bot is unsure how to proceed, or the customer explicitly requests a human’s assistance, Vain Vanessa would be notified. She wishes there was a way to integrate the app she already uses daily onto her website which would satisfy these requirements.

## What Omni Chat Is

Here are Omni Chat's features:

- Users have automatic online statuses so that they needn’t manually set it throughout the day to a potentially incorrect status such as “at the gym”.
- Private chats for conversations between two users.
- Users can see who’s presently typing a message in the chat.
- Users can see if another user is online but not when they were last online. We don't display when a user was last online for the following reasons:
    - It can be a privacy concern.
    - The status is incorrect at times because people may accidentally open the app for the following reasons:
        - They open the app because they mistook it for another one (e.g., mistaking WhatsApp for the phone app).
        - They opened the wrong app because they were in a hurry, and tapped an app icon next to the app they actually wanted to.
        - They happened to have the app open when they unlocked their phone because they hadn't closed it before they last unlocked it. So, it looks like they were recently online but actually they just immediately went to another app.
        - They opened it for a fraction of a second while finding a recent app (e.g., when a person swipes left or right along the bottom edge of the screen to quickly switch between open apps on an iPhone XR).
- Chat messages can be searched.
- When a private chat gets deleted by a user, the messages sent until then are no longer visible to them, and the chat is no longer retrieved when requesting their chats. However, the user they were chatting with still has the chat in the same state it was in before the user deleted it. If the other user sends a message to the user, it shows up as the first message in the user's chat.
- Every message has the date and time it was sent but not when it was delivered or read by other users. We don't have read receipts for the following reasons:
    - If someone needs to know if you saw their message, you should reply with a message like "OK". Otherwise, they'll keep wondering in the back of their minds whether you saw it, and then eventually check the chat (potentially multiple times) for a read receipt.
    - Many times, people need time to reply either because they need to collect their thoughts or they were requested for some data which they'll get soon. Since some people get offended when they see that their question has been read without a reply, people tend to switch off the read receipts feature. However, switching off the read receipts feature just exacerbates the problem because the other person thinks that the user checks and ignores messages regularly.
    - If you need to figure out how long it usually takes a person to reply, the time the message was sent is a more practical means of doing so.
- Messages can be deleted.
- Messages can be starred (this is similar to bookmarking a webpage).
- Messages are formatted using GitHub Flavored Markdown. This allows the average user to easily bolden or italicize words, and technical users to format code snippets, etc. For example, writing "`**Announcement**: College is closed tomorrow`" gets displayed as "**Announcement**: College is closed tomorrow”.
- Messages can be replied to in order to prevent context loss.
- Users can be blocked and unblocked without them knowing.
- The following message types are supported:
    - Text
    - Actions (i.e., buttons which trigger third-party server-side code such as ordering food via a bot)
    - Audio
    - Pictures
    - Polls
    - Videos
    - Group chat invites
    - Docs (i.e., any file type)
- Group chats (e.g., a school class’s chat) have the following features:
    - Multiple admins.
    - If a user is added to a chat, or are added back to a chat after leaving it, they'll be able to see the entire chat's history. This ensures that new participants don't miss older messages.
    - If a user leaves the chat, their messages remain until they delete their account, or rejoin to delete them.
    - Description and icon.
    - Unlimited participants.
    - Broadcast chats (i.e., only admins can message). This option can be toggled for a chat at any time. This is for updates such as a conference's chat where you don't want hundreds of people repeating the same questions.
    - Group chat invites.

        This is useful for something like a college's elective class where hundreds of students from different sections need to be added. Instead of admins manually adding each of them, or manually adding one person from each section who in turn adds their classmates, the admin can simply auto-invite users via a code so that people will forward it to their relevant class's chat.

        This is how it'll work. Every chat gets associated with a UUID (Universally Unique IDentifier). Any user who enters this code gets added to the chat. The code isn't human readable so that hackers can't use brute force to join chats. Whether a chat can be joined via an invitation can be toggled by the admin; except for public chats where invitations are always on.
    - Public chats (e.g., official Android chat, random groups individuals have created, Mario Kart chat). Users can search for, and view public chats without an account. Anyone with an account can join them. A frontend UI may allow for a search engine to index such chats.
    - Though group chats can switch on and off invites, they can't toggle whether the chat is public. This is because making a chat public would cause previously sent messages to become public which is a privacy concern for users who were unaware such a change might be made later on.
- Messages can be forwarded.
- Omni Chat can be self-hosted.

    Citizens of countries which actively block foreign services (e.g., China, Pakistan) can still use it since they can easily run their own server which will obviously be hosted on a different domain, port, etc. which the government cannot block easily.

    Organizations dealing with sensitive data can self-host. Signups can be restricted to users with email address domains allowed by the admin (e.g., john@company.example.com could be allowed but not john@gmail.com). The organization may choose to run the service behind their firewall which prevents the following problems from arising:
    - Hackers getting access to the service.
    - Employees leaking information after-hours.
    - Employees being the target of identity theft after-hours. For example, an employee may have their laptop unlocked while they go to a café’s restroom, or a thief may steal their phone.
- Bots can have buttons so that integrations can easily execute code. For example, if a Travis CI build fails, a bot could message the specifics on the group with a button, which when clicked, automatically reruns the CI/CD pipeline.
- Group audio calls.
- Group video calls.
- Screen sharing.
- Background noise cancellation for audio and video calls.
- Spatial audio during calls. Spatial audio gives a sense of direction for the audio source, which is important for gamers, and even meetings.
- A publicly accessible API allows software developers to build embedded webpage chat integrations, game bots, etc. for the service.

## What Omni Chat Isn’t

Omni Chat isn’t a forum like Spectrum or Stack Overflow. Though such services may include chat in some form, they are fundamentally different from instant messaging, and even the search features are targeted to a completely different audience.

Omni Chat doesn’t have threaded conversations like Slack because in practice having a thread appear out of a previous message during a chat is more confusing than simply replying to the message. If you need threaded discussions, Spectrum is a better alternative since every top-level message is a single-threaded conversation; unlike Slack’s confusing messages where some messages are threads, and some aren’t.

## End-to-End Encryption (E2EE)

Omni Chat doesn’t use E2EE because such apps (e.g., WhatsApp) have the following issues:

- Users must be tied to a single device which acts as a source of truth. WhatsApp, Signal, etc. use a smartphone running a popular OS for this, which many people don't have.
- Once a message has been received, it must live on the device because there’s no way for the server to know which device is requesting it again later on. This causes unnecessary space wastage, especially on mobile devices.
- While the app is uninstalled, or the user is switching to a new phone, they lose any messages sent to them.

Omni Chat has decided it’s better to not take this trade-off for the following reasons:

- Some chats do not benefit from E2EE (e.g., public chats don’t require E2EE since all their data is public).

    If the chat is sensitive enough to require E2EE, a dedicated private communications tool such as ProtonMail or Signal may be used. In this way, the user can benefit from a practical tool (i.e., Omni Chat) most of the time, and only deal with the trade-offs E2EE apps require when needed.

    Though Omni Chat’s primary intent is to eliminate the need for several communication apps, most users will never require this option, and therefore it has been deemed pragmatic. E2EE communications are usually unrelated to chats found in other apps. For example, a journalist may use Signal to message publishers but no one else. Therefore, using an E2EE messaging app separate from Omni Chat doesn't cause the same topic's messages to be scattered across different apps.
- Should data sensitivity be an issue for an organization, self-hosting the service is a great option.
