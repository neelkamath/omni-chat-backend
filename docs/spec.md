# Spec

## The Problem

There are way too many chat apps. Every new one tries to implement features another lacks, but in the process don't implement the features they do. The end result is that users get forced to use several apps, none of which do not serve their purpose well. What's more is that certain people will refuse to use certain apps because they're already on too many. Another issue is that you don't know which app to use for which person at what time for a particular type of message. For example, if Bob wants to send Lisa an important message, he doesn't know whether to use Telegram, Snapchat, etc. because each person checks different apps at different intervals for different types of messages.

## The Solution

A new type of chat app will be created which renders every existing one useless. This will be accomplished by implementing the useful features different apps have, while leaving out the unwanted parts. Omni Chat aims to replace the traditional messaging systems to make email, phone calls, SMS, voicemail, video calls, and just chat in general, manageable for the first time.

There are other products which share our vision, such as Rocket.Chat. We differentiate ourselves with the following points:
- Products like Slack, Zulip, and Rocket.Chat only implement a subset of the features people actually require in chat systems. We implement all of them.
- Products like Slack and Rocket.Chat delude themselves into thinking they'll replace email in the long term. People will never stop using the basics like SMS even if your service gives them free rainbow-pooping unicorns. What we try to do is more humble: replace most of the usage of traditional services with that of our own.
- We don't substitute features with useless integrations which are worse than the original services (e.g., Rocket.Chat's never ending quest of making integrations with companies like Uber even though Uber's app is superior in every way).

Here are examples of existing chat systems that we can replace even though they serve niche use cases:
- AI bots helping customers on websites.
- Bots on sites like Swiggy which serve ready-made FAQs using buttons for navigation.
- Twitch/YouTube's live streaming chat system.

These systems may seem to be highly specialized and out of our reach. However, this isn't the case. These chat systems have embedded components. Our embeddable chat system could be used in their place. Since our app integrates with bots, and can make buttons, we can easily have our system embedded into the same sites and provide the same features. Since our app doesn't require logging in, even people without accounts can easily use it. However, if you are logged in, you'll be able to chat with the Swiggy bot even on your other devices because the chat will be saved to your account.

Many times you only want to switch on notifications for important chats. The user will be able to switch on notifications only for specific chats unlike WhatsApp which only lets you switch them off for specific chats.

Certain systems (e.g., Twitter DMs) will exist even if our app is a success because the creators of those services want to roll their own. The creators of those services will not use our app because they want to be "different" from the other services, even though they'll only build a suboptimal system. We can only hope that users on such platforms will prefer to exchange usernames to chat on our platform.

The core software will be open source so that user requiring FOSS (e.g., open source enthusiasts, organizations requiring data to be hosted on their servers) will be able to use the service. Although you can host your own instance, there will be the default free public instance nontechnical users, or people who are just trying it out, use.

## Features

Checkboxes indicate which features have been implemented.

Since this repo is for the backend API, here are recommendations for a developer creating a frontend UI:
- Messages consisting solely of a single emoji should have the emoji enlarged.
- You can require a password or biometric to unlock the app.
- Allow the user to draw messages in-app. You could convert it to an image before sending.
- Allow the user to send their location in-app. For example, an option to send their location via a Google Maps URL could be included.
- Show link previews so that URLs show relevant info about websites shared in messages, such as a Google Maps URL. For example, you could use [SwiftLinkPreview](https://github.com/LeonardoCardoso/SwiftLinkPreview) for iOS.
- Allow users to view certain doc messages, such as PDFs, in-app if feasible.
- When sharing a group chat invite code, allow the option to share it via a QR code so that people can see it offline. For example, a user could print and stick the QR code to their classroom's door so that people can easily join the chat.
- There should be a section which allows users to enter a group chat invite code, or scan a QR code containing a group chat invite code, to join shared chats.
- Public chats should be indexable by search engines so that users without an account can search for messages, etc. This could be done by giving every message a URL (e.g, a message with ID `1` in a chat with ID `4` could have a URL `https://example.com/chats/4/1`). Omni Chat instances meant for internal company use may not want this, so indexability should be configurable.
- When a user who isn't in the user's contacts creates a chat with the user, it should go to an "Invites" tab where the user will have to go to ignore the chat, delete the chat, block the user, or add them to their contacts so that the chat will appear in the "Chats" tab. Otherwise, users and bots might spam each other, especially if they're a celebrity.

### Free

Features must be free if they're a unique selling point, are useful for the average user, or if they're already free in another popular service (e.g., video-mail in Google Duo).

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
- [ ] Forum chats. These are for single threaded chats, like a team's communication channel. Every message which isn't a reply to another message becomes a thread, and people can reply to this thread so that relevant info can be easily searched in the future. There will only be one thread per top-level message (i.e., a reply to a reply will still be a reply to the top-level message as well) to prevent irrelevant replies.
- [ ] E2E Encryption.
- [ ] Group audio calls.
- [ ] Spatial audio calls (important for gamers).
- [ ] Video calls with multiple people.
- [ ] Screen sharing.
- [ ] Background noise cancellation for both audio and video calls.
- [ ] API for integrations (e.g., bots).
- [ ] Allow instances to pick one of the following access types:
    - [ ] Only clients an admin creates are allowed to access the API.
    - [ ] Anyone can access the API without registering.
    - [ ] Anyone can access the API, but must register for an account. The accounts will be automatically created, but this allows for features such as rate limiting, and payments.
- [ ] Bots can have buttons so that integrations can easily execute code. For example, if a Travis CI build fails, a bot could message the specifics on the group with a button, which when clicked, automatically reruns the CI/CD pipeline.
- [ ] Run a production instance taking care of the following:
    - [ ] Don't run images as the `root` user.
    - [ ] Automate backups.
    - [ ] Use Docker secrets instead of an `.env` file. At the very least, doc that you should use secrets instead of an `.env` file (if it's bad to use an `.env` file).
    - [ ] Use Docker configs instead of bind mounts.
    - [ ] Metrics via Prometheus and/or [Elastic Metrics](https://www.elastic.co/infrastructure-monitoring).
    - [ ] Monitoring and tracing via [Elastic APM](https://www.elastic.co/apm).
    - [ ] Logs consumed by [Elastic Logs](https://www.elastic.co/log-monitoring).

### Paid

- [ ] Auto-reply suggestions.
- [ ] Disallow screenshots on certain chats.
- [ ] The citizens of Oman have to use a VPN to use WhatsApp video calls because their government had it blocked. It could also have an automatic VPN so that places like Oman can't block its services. This can be paid because it's not a unique selling point; even external VPNs would work.
- [ ] Create verified people and groups, so people know that they're joining the official Android group chat. You'll need to pay to get verified, but of course free users can see who is verified.
- [ ] Allow chats to be embedded on websites and mobile apps.
- [ ] Realtime translation for text, audio, and video.
- [ ] Builtin suggestions for restaurants you're talking about, etc. (i.e., a personal assistant like the one Allo has).
- [ ] Have a default voicemail, or video-mail message. For example, you could set up voicemail to tell people you're currently on vacation.
- [ ] UI themes.
- [ ] Even nontechnical users can build bots for things like Swiggy chat integrations (e.g., a bot which serves FAQs via buttons).
- [ ] It should easily embed into existing systems so that Twitter, YouTube, Zomato, etc. won't want to roll their own. 

### Ideas for the Far-Off Future

The following ideas will only be worked on if it makes sense after the full chat app is working.
- [ ] Gamified leaderboard for the amount of time you spend on the app, and the three people you talk to the most.
- [ ] Stories (i.e., statuses which disappear after 24h).
- [ ] View content from verified and individual creators.
- [ ] Instead of slow random semi-public tweets or FB posts, companies like Uber can easily set up customer support on our app. They don't even need to use it as an additional support channel because anyone can use it anonymously without an account. This way people with grievances can simply give them the required proof over chat, and never have to install the app.

With these features, we might even be able to replace social media sites, and some infrastructure required by call centers. Unlike Instagram and Snapchat, which only replaced half of social media usage, our app will try to go all the way.