# Configuration

1. Create a file named `Caddyfile`. Copy-paste the contents of the [example `Caddyfile`](Caddyfile) into it.

    Leave the file as it is if you're running Omni Chat locally. You're probably going to run it locally if you're a dev contributing to it, a sysadmin trying it out to see if you want to run it, or a frontend dev building a GUI for it.
    
    If you're running in production, replace the first line with your domain name (e.g., replace `:80` with `example.com`) so that HTTPS can automatically be enabled. Since passwords get transferred, you must only use the HTTPS protocol when exposing the application. The application gets exposed over HTTP as well, but will automatically redirect to HTTPS. The HTTP protocol gets used internally, which is a nonissue.
1. Create a file named `.env`. Copy-paste the key-value pairs from the [example `.env`](.env) into it, and set your own values. Keys starting with `KEYCLOAK_SMTP` regard the email service provider you choose to automate emails with (e.g., a new user would automatically receive an email to verify their email address). Here's an explanation of the keys:

    |Key|Explanation|
    |---|---|
    |`CHAT_DB_PASSWORD`|The password for the chat system's DB.|
    |`KEYCLOAK_DB_PASSWORD`|The password for the auth system's DB.|
    |`KEYCLOAK_PASSWORD`|The password for the auth system's admin panel.|
    |`KEYCLOAK_CLIENT_SECRET`|The password used internally for the chat system to programmatically access the auth system.|
    |`KEYCLOAK_SMTP_HOST`|Your email service provider's hostname.|
    |`KEYCLOAK_SMTP_TLS_PORT`|The TLS port your email service provider runs on.|
    |`KEYCLOAK_SMTP_USER`|Your email account's username.|
    |`KEYCLOAK_SMTP_PASSWORD`|Your email account's password. If your email account requires multi-factor authentication, you'll have to use an app password (e.g., [Gmail app password](https://support.google.com/accounts/answer/185833?hl=en)).|
    |`JWT_SECRET`|The password used internally to create JWTs.|
    |`ALLOWED_DOMAINS`|Since Omni Chat can be deployed by anyone, a company might want to use it for internal communications, and hence disallow non-employees from using their instance. This can be achieved by specifying which email address domains (i.e., the part of the email address after the `@`) are allowed during sign-ups. If you don't have this use case, simply excluding this key-value pair from the `.env` file will allow every email address domain to be used. Otherwise, set the value to a comma-separated list of email address domains (e.g., `private.company.com,gmail.com`).|