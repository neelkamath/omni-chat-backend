# Environment Variables

Keys starting with `SMTP` are for the email service. Omni Chat sends the user an email either when they need to verify their email address or reset their password.

You can run Omni Chat either by [using Docker Compose](docker-compose.md) or [in the cloud](cloud.md). If you're setting environment variables using the Docker Compose guide, only set the ones indicated by the **Docker Compose** column in the following table. If you're using the Cloud guide, set all the variables.

|Docker Compose|Key|Explanation|
|:---:|:---:|---|
|Yes|`APP_NAME`|The app name will be used in emails sent to the user. Though this API is named Omni Chat, the frontend UI may choose a different name because the end user doesn't know which APIs are being used under-the-hood. For example, when an email is sent to ask the user to verify their email address, the email will say something like "Here's the code to verify your Hermes account's email address.", where _Hermes_ is the `APP_NAME`.|
|Yes|`JWT_SECRET`|The password used internally to create JWTs.|
|No|`POSTGRES_DB`|DB name (e.g., `my_db`).|
|No|`POSTGRES_USER`|Superuser's username.|
|Yes|`POSTGRES_PASSWORD`|Superuser's password.|
|No|`POSTGRES_URL`|For example, the URL would be `db:5432` if the hostname and port were `db` and `5432` respectively.|
|No|`REDIS_URL`|For example, the URL would be `redis://message-broker:6379` if the hostname and port were `message-broker` and `6379` respectively.|
|Yes|`ALLOWED_EMAIL_DOMAINS`|Since Omni Chat can be deployed by anyone, a company might want to use it for internal communications, and hence disallow non-employees from using their instance. This can be achieved by specifying which email address domains (i.e., the part of the email address after the `@`) are allowed during sign-ups. If you don't have this use case, simply leaving the value blank for this key will allow every email address domain to be used. Otherwise, set the value to a comma-separated list of email address domains (e.g., `example.com,gmail.com`).|
|Yes|`SMTP_HOST`|The hostname of your email service provider (e.g., `smtp.gmail.com` for Gmail).|
|Yes|`SMTP_TLS_PORT`|The TLS port your email service provider runs on (e.g., `587` for Gmail).|
|Yes|`SMTP_FROM`|You email address which will be used in the _from_ header of emails sent to users.|
|Yes|`SMTP_USERNAME`|Your email account's username.|
|Yes|`SMTP_PASSWORD`|Your email account's password. If you're using multifactor authentication, you'll need to create an app password (e.g., [Google App Password](https://support.google.com/accounts/answer/185833)).|
