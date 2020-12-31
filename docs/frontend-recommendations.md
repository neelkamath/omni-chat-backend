# Recommendations for Frontend UI Developers

Since this repo is for the backend API, here are recommendations for a developer creating a frontend UI:

- Have a _Developers_ section which links to the URL Omni Chat is running on, and Omni Chat's [docs](api.md) so that
  third party developers can create and run bots, etc. for your app.
- Messages consisting solely of a single emoji should have the emoji enlarged.
- Optionally require a password or biometric to unlock the app.
- Allow the user to draw messages in-app. You could convert it to an image before sending.
- Allow the user to send their location in-app. For example, an option to send their location via a Google Maps URL
  could be included.
- Show link previews so that URLs show relevant info about websites shared in messages, such as a Google Maps URL. For
  example, you could use [SwiftLinkPreview](https://github.com/LeonardoCardoso/SwiftLinkPreview) for iOS.
- Allow users to view certain doc messages such as PDFs in-app if feasible.
- When sharing a group chat invite code, allow the option to share it via a QR code so that people can see it offline.
  For example, a user could print and stick the QR code to their classroom's door so that people can easily join the
  chat.
- There should be a section which allows users to enter a group chat invite code, or scan a QR code containing a group
  chat invite code, to join shared chats.
- Public chats should be indexable by search engines so that users without an account can search for messages, etc. This
  could be done by giving every message a URL (e.g, a message with ID `1` in a chat with ID `4` could have a
  URL `https://example.com/chats/4/1`). Omni Chat instances meant for internal company use may not want this, so
  indexability should be configurable.
- When a user who isn't in the user's contacts creates a chat with the user, it should go to an "Invites" tab where the
  user will have to go to ignore the chat, delete the chat, block the user, or add them to their contacts so that the
  chat will appear in the "Chats" tab. Otherwise, users and bots might spam each other, especially if they're a
  celebrity.
- Many times you only want to switch on notifications for important chats. The user will be able to switch on
  notifications only for specific chats unlike WhatsApp which only lets you switch them off for specific chats.
