# Configuration

Create a file named `.env`. Copy-paste the key-value pairs from the [example `.env`](.env) into it, and set your own values. The `.env` file is prefilled with valid values (i.e., the email account's username, password, etc. are valid credentials for a test account created to help you try out Omni Chat more easily). Keys starting with `SMTP` are for the email service. Omni Chat sends the user an email when they need to verify their email address, or reset their password. Here's an explanation of the keys:

|Key|Explanation|
|---|---|
|`APP_NAME`|The app's name. This doesn't have to be _Omni Chat_ because which APIs are used under the hood are unknown to end users. The app name will be used in emails sent to the user. For example, when an email is sent to ask the user to verify their email address, the email will say something like "Here's the code to verify your Hermes account's email address.", where _Hermes_ is the `APP_NAME`.|
|`DB_PASSWORD`|The password for the chat system's DB.|
|`JWT_SECRET`|The password used internally to create JWTs.|
|`ALLOWED_DOMAINS`|Since Omni Chat can be deployed by anyone, a company might want to use it for internal communications, and hence disallow non-employees from using their instance. This can be achieved by specifying which email address domains (i.e., the part of the email address after the `@`) are allowed during sign-ups. If you don't have this use case, simply leaving the value blank for this key will allow every email address domain to be used. Otherwise, set the value to a comma-separated list of email address domains (e.g., `private.company.com,gmail.com`).|
|`SMTP_HOST`|The hostname of your email service provider (e.g., `smtp.gmail.com` for Gmail).
|`SMTP_TLS_PORT`|The TLS port your email service provider runs on (e.g., `587` for Gmail).|
|`SMTP_FROM`|You email address which will be used in the _from_ header of emails sent to users.|
|`SMTP_USERNAME`|Your email account's username.|
|`SMTP_PASSWORD`|Your email account's password. If you're using multifactor authentication, you'll need to create an app password (e.g., [Google App Password](https://support.google.com/accounts/answer/185833)).|
