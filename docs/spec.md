# Spec

## The Problem

There are way too many chat apps. Every new one tries to implement features another lacks, but in the process don't implement the features they do. The end result is that users get forced to use several apps, none of which do not serve their purpose well. What's more is that certain people will refuse to use certain apps because they're already on too many. Another issue is that you don't know which app to use for which person at what time for a particular type of message.

## The Solution

We'll create a new type of chat app which must render all the existing ones useless. It's important we go all the way. Otherwise, we'll just be another chat app no one will want to use a few years from now. We'll accomplish this goal by implementing the useful features different apps have, while leaving out the bad stuff. We aim to replace the traditional messaging systems to make email, phone calls, SMS, voicemail, video calls, and just chat in general, manageable for the first time.

There are other services who share our vision, such as Rocket.Chat. We differentiate ourselves with the following points.
- Services like Slack, Zulip, and Rocket.Chat only implement a subset of the features people actually require in chat systems. We implement all of them.
- Services like Slack and Rocket.Chat delude themselves into thinking that they'll replace email in the long term. People will never stop using the basics like SMS even if your service gives them free rainbow-pooping unicorns. What we try to do is more humble: replace most of the usage of traditional services with that of our own.
- We don't substitute features with useless integrations which are worse than the original services (e.g., Rocket.Chat's neverending quest of making integrations with companies like Uber even though Uber's app is superior in every way).

Here are examples of existing chat systems that we can replace even though they serve niche use cases.
- AI bots helping customers on websites.
- Bots on sites like Swiggy which serve ready-made FAQs using buttons for navigation.
- Twitch/YouTube's live streaming chat system.

These systems may seem to be highly specialized and out of our reach. But this is not the case. These chat systems are  embedded components. Our embeddable chat system could be used in their place. Since our app integrates with bots, and can make buttons, we can easily have our system embedded into the same sites and provide the same features. Since our app doesn't require logging in, even people without accounts can easily use it. If you are logged in though, you'll be able to chat with the Swiggy bot even on your other devices because the chat will be saved to your account.

Certain systems (e.g., Twitter DMs) will exist even if our app is a success because the creators of those services want to roll their own. The creators of those services will not use our app because they want to be "different" from the other services, even though they'll only build a suboptimal system. We can only hope that users on such platforms will prefer to exchange usernames to chat on our platform.

## Implementation Notes for the User

### Accounts

- The same email address cannot be registered twice.
- Deleting your account results in all your db (e.g., every message you've ever sent) to be wiped.

- A message can have one of three states. A _sent_ message has been uploaded to the server. A _delivered_ message has been downloaded by the other user. A _read_ message has been seen by the other user.

### Private Chats

- A _private chat_ is a chat between two users which cannot be converted into a group chat. 
- When a private chat is deleted by a user, the messages sent until then are no longer visible to them, and the chat is no longer retrieved when requesting their chats. However, the user they were chatting with will still have the chat in the same state it was in before the user deleted it. If the other user sends a message to the user, it will show up as the first message in the user's chat.

### Forum Chats

- Threaded chats like those in Spectrum and Slack.

### Group Chats

- If you are added to a group chat, or are added back to a group chat after leaving it, you will be able to see the entire chat's history.
- If you leave a group chat, your messages will remain in the chat.

## Features

- The core software will be open source so that user requiring libre software (e.g., idealist hobbyists, companies requiring code control, organizations requiring private deployments on their own servers) will be able to use the service. 
- Although you can host your own instance, there will be the default free public instance nontechnical users, or people who are just trying it out, will use.
- You can chat with an account, with an account anonymously (so that you can have your chats saved without others knowing your real-life identity), and as a guest without an account.
- Features marked as **PAID** are close-sourced. Features must be free if they are a unique selling point, are useful for the common man, or if they are already free in another popular service (e.g., videomail in Google Duo).
- Checkboxes indicate which features have been implemented.
- Features are ordered by priority in each category.

### Customization

- [ ] The user who creates the group is the admin. There can only be one admin in a group. If the admin decides to leave the group, they will have to appoint a member to become the admin once they leave (unless they are the only member left). Regular users can only send messages. Moderators can do everything a regular user can do, add/remove users/bots, and update the group's title/description. Admins can do everything a moderator can do in addition to add/remove moderators, and delete the group. The admin can make everyone in the group a moderator if they wish it.
- [ ] You'll be able to switch on notifications for specific chats unlike WhatsApp which only lets you switch them off for specific chats.
- [ ] API for integrations (e.g., bots).
- [ ] Bots can have buttons so that integrations can execute code easily. For example, if a Travis CI build fails, a bot could message the specifics on the group with a button, which when clicked, automatically reruns the CI/CD pipeline.
- [ ] **PAID** Disallow bots for a particular chat. Of course, if the chat is configured such that only the admin can add users, then even free users can effectively disallow bots. Chats should probably disallow bots by default. We should also probably disallow bots to message users unless they're verified by us (e.g., only services like BookMyShow should be able to automatically message people).
- [ ] **PAID** Specific chats can disable certain features. For example, a customer support bot might not accept drawings and video calls, so those options won't show up. Another example is that a particular group chat doesn't want images to show up, and hence configures its chat to convert image uploads to Imgur links. It is sufficient if the creator of the group has disabled these features.
- [ ] **PAID** Disable features you don't want on your instance (e.g., filters).
- [ ] **PAID** Even nontechnical users can build bots for things like Swiggy chat integrations (e.g., a bot which serves FAQs via buttons).
- [ ] **PAID** It should easily embed into existing systems so that Twitter, YouTube, Zomato, etc. won't want to roll their own. 

### General

- [ ] Although we allow users to set only particular chats' notifications on, it still won't be enough to know if there's an important message from a particular chat. For example, your mom might only text you occasionally - half the time it's important, and half the time she's just sharing a joke. To deal with this case, we'll have each message associated with a priority. The priorities are:
    - Urgent: Needs attention within a minute (e.g., need help filling out a time-sensitive bank form)
    - Important: Needs attention within an hour (e.g., need to know what the HW assignment is so I can start before I go to bed)
    - Useful: Need attention within this day (e.g., need to schedule a meeting for tomorrow; can be scheduled any time today)
    - Default: Doesn't need attention within this day
    
    By default, messages are sent with the _Default_ priority so as to not waste time picking priorities on every text. By default, priorities will be honored. If the user finds other user's ideas of priority incorrect, then they can choose to ignore priorities associated with messages for all chats, individual chats, or all users except handpicked chats.
- [ ] See saved contacts from both people you've chatted with, and people you've saved but haven't yet chatted with. For example, your phone book or mail book might automatically find contacts which are on the service by searching and saving them for you via searching their phone numbers or emails if they're listed publicly. 
- [ ] For privacy purposes, chats with only two people (you and the other person) cannot be converted into group chats by adding other users. You can however start a chat with two people as a group chat so that new users could be added later. This way chats between two people could easily become visible by potential future team members.
- [ ] Search users based on their public information. For example, if a user's phone number and email is public, you can see if your friend is on the service using that in the case that you don't know their username. Useful information can be displayed publicly, such as an organization they went to (e.g., you can search users using Google or Stanford as an organization filter if they went/go there).
- [ ] Automatic online status. You don't manually set whether you're "away" or some other error-prone status that no one takes seriously.
- [ ] Public and private DMs and group chats.
- [ ] See who else is in the group.
- [ ] Group descriptions and icons.
- [ ] Pinned messages.
- [ ] Show if the person is looking at your chat right now rather than only whether they're typing or online. This should be shown at the bottom of the screen. Otherwise, people need to move their eyes to the top to see the other person's status even though the messages appear at the bottom.
- [ ] Search for chats (e.g., official Android chat, random groups individuals have created).
- [ ] Unlimited people per group.
- [ ] Archive chats.
- [ ] Broadcast groups where only admins can send messages. This way you don't need to worry about spam from hundreds of idiots asking stupid questions, and you can keep notifications on for when they actually matter.
- [ ] By default, anonymous and accountless users are disallowed from groups.
- [ ] Every chat and message has a URL.
- [ ] Allow search engines to index the chat.
- [ ] You can toggle between online and offline mode. Messages and calls will send via your SIM card in offline mode. Similar to how FaceTime and iMessage makes calls and messages free by converting SIM usages to internet messages under the hood, we could provide free offline (SIM use) services.
- [ ] Background noise cancellation for audio and video calls. This needs to be free so that people like homemakers will prefer it over WhatsApp's shitty service.
- [ ] Make it a federated chat system so that you can chat with people on other instances. Let's say that there are two instances, `instance1` and `instance2`. `instance1` has two users, `black` and `gray`. `instance2` has a  user `pink`. If `blue` wanted to message `gray`, `blue` could either specify `gray`'s username as `@instance1/gray` or `@gray`. If `blue` wanted to message `purple`, `blue` would have to specify `purple`'s username as `@instance2/purple`.
- [ ] Marketplace for free and paid bots, themes, digital stickers, etc.
- [ ] **PAID** Embeddable chat on webpages, and in mobile apps.
- [ ] **PAID** Realtime translation for text, audio, and video.
- [ ] **PAID** Builtin suggestions for restaraunts you're talking about, etc. (i.e., a personal assistant like the one Allo has).

### Personalization

- [ ] Easily switch between multiple accounts. This can't be paid because we need to allow people to have the free features provided by services like IRC.
- [ ] **PAID** Have a default voicemail, or videomail message. For example, you could set up voicemail to tell people you're currently on vacation.
- [ ] **PAID** Themes.

### Messaging

- [ ] We'll have unlimited chat history so that new joinees don't miss older messages.
- [ ] Every message has the date and time it was sent, delivered, and read.
- [ ] Markdown support.
- [ ] Emoji will be larger than usual if the entire message is a single emoji.
- [ ] Reply to a message so that the context isn't lost.
- [ ] Send drawings, contacts, locations, live locations for a specified duration, audio recordings, photos, videos, memoji, stickers, and any file type.
- [ ] Star messages. If you open a chat's starred messages, you will see only the messages you starred in that chat. If you open your starred messages globally, you'll see every chat's starred messages. Clicking on a starred message will take you to that point in history in the respective chat. Before you delete a chat, it'll ask you what to do with your starred messages. Deleted messages will automatically disappear from people's starred messages.
- [ ] Search chat.
- [ ] Polls.
- [ ] Use @username or @all for notifications.
- [ ] **PAID** Auto-reply suggestions.

### Audio calls

- [ ] Group calls.
- [ ] Spatial audio (important for gamers).

### Video Calls

- [ ] Videomail (voicemail for video).
- [ ] Video calls with multiple people.
- [ ] Filters. The others will be able to see which filter you're using.
- [ ] Screensharing.
- [ ] Have add-ons for video calls like games, and bunny ear effects.

### Security

- [ ] Allow instances to pick one of the following access types.
    - [ ] Only clients an admin creates are allowed to access the API.
    - [x] Anyone can access the API without registering.
    - [ ] Anyone can access the API, but must register for an account. The accounts will be automatically created, but this allows for features such as rate limiting, or payments.
- [ ] Allow people you don't know to not directly contact you (i.e., they will go to an "Invites" tab where you'll have to go to allow, ignore, or block them).
- [ ] Allow certain people (e.g., family, friends) to always see your live location (or at least during certain preconfigured times).
- [ ] Allow people without an account (or people who have an account who wish to remain anonymous) to message in group chats.
- [ ] Encryption.
- [ ] Report someone.
- [ ] **PAID** Create verified people and groups, so people know that they're joining the official Android group chat. You'll need to pay to get verified, but of course free users can see who is verified.

### Privacy

- [ ] Share as much, or as little, information as you want. You can share your address, phone, email, profile photo, etc. You can choose who can see what info. If a bot is messaging you, it'll have to ask for permissions for every type of db it wants to access.
- [ ] Chat without an account.
- [ ] View chat even if you're not logged in. Of course, this will only be allowed if it's a public chat anyone can join.
- [ ] Incognito chats whose messages automatically disappear after a set amount of time, or shortly after the user reads them.
- [ ] Delete messages only for yourself that you've sent. Delete messages others have sent only for yourself. Delete messages you've sent for everybody.
- [ ] Choose who can see when you were last online, or whether they saw your message.
- [ ] You can toggle chat history so that it automatically gets deleted in a particular manner (e.g., new participants can't see old messages).
- [ ] App-wide, and chat-specific lock. You will need to enter a password or biometric to unlock the app or chat.
- [ ] **PAID** Disallow screenshots on certain chats.
- [ ] **PAID** The citizens of Oman have to use a VPN to use WhatsApp video calls because their government had it blocked. It could also have an automatic VPN so that places like Oman can't block its services. This can be paid because it's not a unique selling point; even external VPNs would work.

## Ideas for the Far-off Future

The following ideas will only be worked on if it makes sense after the full chat app is working.
- [ ] Gamified leaderboard for the amount of time you spend on the app, and the three people you talk to the most.
- [ ] Stories (i.e., statuses which disappear after 24h).
- [ ] View content from verified and individual creators.
- [ ] Instead of slow random semi-public tweets or FB posts, companies like Uber can easily set up customer support on our app. They don't even need to use it as an additional support channel because anyone can use it anonymously without an account. This way people with grievances can simply give them the required proof over chat, and never have to install the app.

With these features, we might even be able to replace social media sites, and some of the infrastructure required by call centers. Unlike Instagram and Snapchat, which only replaced half of social media usage, our app will try to go all the way.

## Implementation

Features will be implemented in the following order.
1. Free open-sourced features
1. Paid close-sourced features
1. Ideas for the far-off future