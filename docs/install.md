# Installation

1. Install [Docker](https://hub.docker.com/search/?type=edition&offering=community).
1. If you're developing the project, install the following:
    - [OpenJDK 14 (HotSpot)](https://adoptopenjdk.net/?variant=openjdk14&jvmVariant=hotspot)
    - The latest [node.js](https://nodejs.org/en/download/) LTS.
1. Clone the repository using one of the following methods.
    - SSH: `git clone git@github.com:neelkamath/omni-chat.git`
    - HTTPS: `git clone https://github.com/neelkamath/omni-chat.git`
1. `cd omni-chat`
1. Create a file named `Caddyfile`. Copy-paste the contents of [`docs/Caddyfile`](Caddyfile) into it. If you're developing or trying out the app, leave the file as it is. If you're running in production, replace the first line with your domain name (e.g., replace `:80` with `example.com`) so that HTTPS can automatically be enabled. Since passwords get transferred, you must only use the HTTPS protocol when exposing the application. The HTTP protocol gets used internally, which is a nonissue.
1. Create a file named `.env`. Copy-paste the key-value pairs from [`docs/.env`](.env) into it, and personalize the values. Keys starting with `KEYCLOAK_SMTP` regard the email service provider you choose to automate emails with (e.g., a new user gets emailed to verify their email address). Here's an explanation of the keys:

    |Key|Explanation|
    |---|---|
    |`CHAT_DB_PASSWORD`|The password for the chat system's DB.|
    |`KEYCLOAK_DB_PASSWORD`|The password for the auth system's DB.|
    |`KEYCLOAK_PASSWORD`|The password for the auth system's admin panel.|
    |`KEYCLOAK_CLIENT_SECRET`|The password used internally for the chat system to programmatically access the auth system.|
    |`KEYCLOAK_SMTP_HOST`|Your email service provider's hostname.|
    |`KEYCLOAK_SMTP_TLS_PORT`|The TLS port your email service provider runs on.|
    |`KEYCLOAK_SMTP_USER`|Your email account's username.|
    |`KEYCLOAK_SMTP_PASSWORD`|Your email account's password.|
    |`JWT_SECRET`|The password used internally to create JWTs.|