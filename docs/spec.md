# Spec

## The Problem

There are too many chat apps. Every new one tries to implement features another lacks, but in the process don't implement the features they do. The end result is that users get forced to use several apps, none of which serve their purpose well. What's more is that certain people will refuse to use certain apps because they're already on too many. Another issue is that you don't know which app to use for which person at what time for a particular type of message. For example, if Bob wants to send Lisa an important message, he doesn't know whether to use Telegram, Snapchat, etc. because each person checks different apps at different intervals for different types of messages.

There are currently only two good chat apps, [Discord](http://discord.com) and [Signal](https://www.signal.org/). There are other decent apps like WhatsApp, but the aforementioned apps provide more features than its competitors provide, and hence we'll only focus on them for comparing Omni Chat with competitors. Discord, Signal, and Omni Chat all have group video calls, etc., so we'll focus on the non-common features:

|App|E2E Encryption|Screen Sharing|Open source|
|:---:|:---:|:---:|:---:|
|Discord|❌|✅|❌|
|Signal|✅|❌|✅|
|Omni Chat|❌|✅|✅|

## Recommendations for Frontend UI Developers

Since this repo is for the backend API, here are recommendations for a developer creating a frontend UI:
- Have a _Developers_ section which links to the URL Omni Chat is running on, and Omni Chat's [docs](api.md) so that third party developers can create and run bots, etc. for your app.
- Messages consisting solely of a single emoji should have the emoji enlarged.
- Optionally require a password or biometric to unlock the app.
- Allow the user to draw messages in-app. You could convert it to an image before sending.
- Allow the user to send their location in-app. For example, an option to send their location via a Google Maps URL could be included.
- Show link previews so that URLs show relevant info about websites shared in messages, such as a Google Maps URL. For example, you could use [SwiftLinkPreview](https://github.com/LeonardoCardoso/SwiftLinkPreview) for iOS.
- Allow users to view certain doc messages such as PDFs in-app if feasible.
- When sharing a group chat invite code, allow the option to share it via a QR code so that people can see it offline. For example, a user could print and stick the QR code to their classroom's door so that people can easily join the chat.
- There should be a section which allows users to enter a group chat invite code, or scan a QR code containing a group chat invite code, to join shared chats.
- Public chats should be indexable by search engines so that users without an account can search for messages, etc. This could be done by giving every message a URL (e.g, a message with ID `1` in a chat with ID `4` could have a URL `https://example.com/chats/4/1`). Omni Chat instances meant for internal company use may not want this, so indexability should be configurable.
- When a user who isn't in the user's contacts creates a chat with the user, it should go to an "Invites" tab where the user will have to go to ignore the chat, delete the chat, block the user, or add them to their contacts so that the chat will appear in the "Chats" tab. Otherwise, users and bots might spam each other, especially if they're a celebrity.
- Many times you only want to switch on notifications for important chats. The user will be able to switch on notifications only for specific chats unlike WhatsApp which only lets you switch them off for specific chats.

## Features

Checkboxes indicate which features have been implemented.

- [x] Automatic online status. You don't manually set whether you're "away", or some other error-prone status that you have to constantly update, and no one takes seriously.
- [x] Private chats. These are for conversations between two people, like you and your friend.
- [x] See who's typing in the chat.
- [x] See when someone's online, or when they were last online.
- [x] Search the chat.
- [x] When a private chat gets deleted by a user, the messages sent until then are no longer visible to them, and the chat is no longer retrieved when requesting their chats. However, the user they were chatting with will still have the chat in the same state it was in before the user deleted it. If the other user sends a message to the user, it will show up as the first message in the user's chat.
- [x] Every message has the date and time it was sent, delivered, and read for each user.
- [x] Delete messages.
- [x] Star messages.
- [x] Markdown support.
- [x] Reply to a message to prevent context loss.
- Message types:
    - [x] Text
    - [x] Actions (i.e., buttons which trigger third-party server-side code such as ordering food via a bot)
    - [x] Audio
    - [x] Pictures
    - [x] Polls
    - [x] Videos
    - [x] Group chat invites
    - [x] Doc (i.e., any file type)
- Group chats, like your school class's chat.
    - [x] Multiple admins.
    - [x] If you are added to a chat, or are added back to a chat after leaving it, you'll be able to see the entire chat's history so that new participants don't miss older messages.
    - [x] If you leave a chat, your messages will remain in the chat until you delete your account, or join the chat again to delete them.
    - [x] Descriptions and icons.
    - [x] Unlimited participants.
    - [x] Broadcast chats (i.e., only admins can message). This option can be toggled for a chat any time. This is for chats for updates, like a conference's chat where you don't want hundreds of people asking the same questions over and over again.
    - [x] Group chat invite codes.
    
        This is useful for something like a college's elective class where hundreds of students from different sections need to be added. Instead of admins manually adding each of them, or manually adding one person from each section who in turn adds their classmates, the admin can simply auto-invite users via a code so that people will forward it to their relevant section's chat.
        
        This is how it'll work. Every chat gets associated with a UUID (Universally Unique IDentifier). Any user who enters this code gets added to the chat. The code isn't human readable so that hackers can't use brute force to join chats. Whether a chat can be joined via an invitation can be toggled by the admin; except for public chats where invitations are always on.
    - [x] Public chats (e.g., official Android chat, random groups individuals have created, Mario Kart chat). People can search for, and view public chats without an account. Anyone with an account can join them. A frontend UI may allow for a search engine to index the chat should the administrator allow for it.
- [x] Forward messages.
- [x] Omni Chat can be deployed for private use as well. For example, a company may only want to use it as an internal platform, in which case they can specify that only certain email address domains can create accounts. This way, even if an intruder gets into the company's network, they won't be able to create an account since they won't have a company issued email address. This feature also prevents employees from creating an account with their personal email address.
- [x] Bots can have buttons so that integrations can easily execute code. For example, if a Travis CI build fails, a bot could message the specifics on the group with a button, which when clicked, automatically reruns the CI/CD pipeline.
- [ ] Group audio calls.
- [ ] Group video calls.
- [ ] Screen sharing.
- [ ] Background noise cancellation for both audio and video calls.
- [ ] Spatial audio calls (important for gamers).
