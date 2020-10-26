CREATE EXTENSION pgcrypto;
CREATE TYPE message_status AS ENUM ('delivered', 'read');
CREATE TYPE pic_type AS ENUM ('png', 'jpeg');
CREATE TYPE audio_type AS ENUM ('mp3', 'mp4');
CREATE TYPE message_type AS ENUM ('text', 'action', 'pic', 'audio', 'video', 'doc', 'poll', 'group_chat_invite');
CREATE TYPE group_chat_publicity AS ENUM ('not_invitable', 'invitable', 'public');
CREATE TABLE chats
(
    id SERIAL PRIMARY KEY
);
CREATE TABLE pics
(
    id   SERIAL PRIMARY KEY,
    pic  BYTEA    NOT NULL,
    type pic_type NOT NULL
);
CREATE TABLE users
(
    id                              SERIAL PRIMARY KEY,
    username                        VARCHAR(30)   NOT NULL UNIQUE,
    password_digest                 VARCHAR(64)   NOT NULL,
    password_reset_code             INTEGER       NOT NULL,
    email_address                   VARCHAR(254)  NOT NULL UNIQUE,
    has_verified_email_address      BOOLEAN       NOT NULL,
    email_address_verification_code INTEGER       NOT NULL,
    first_name                      VARCHAR(30)   NOT NULL,
    last_name                       VARCHAR(30)   NOT NULL,
    is_online                       BOOLEAN       NOT NULL,
    last_online                     TIMESTAMP,
    bio                             VARCHAR(2500) NOT NULL,
    pic_id                          INTEGER REFERENCES pics (id)
);
CREATE TABLE group_chats
(
    id           INTEGER              NOT NULL UNIQUE REFERENCES chats (id),
    title        VARCHAR(70)          NOT NULL,
    description  VARCHAR(1000)        NOT NULL,
    pic_id       INTEGER REFERENCES pics (id),
    is_broadcast BOOLEAN              NOT NULL,
    publicity    group_chat_publicity NOT NULL,
    invite_code  UUID UNIQUE
);
CREATE TABLE messages
(
    id                 SERIAL PRIMARY KEY,
    chat_id            INTEGER      NOT NULL REFERENCES chats (id),
    sent               TIMESTAMP    NOT NULL,
    sender_id          INTEGER      NOT NULL REFERENCES users (id),
    type               message_type NOT NULL,
    has_context        BOOLEAN      NOT NULL,
    context_message_id INTEGER REFERENCES messages (id),
    is_forwarded       BOOLEAN      NOT NULL
);
CREATE TABLE text_messages
(
    message_id INTEGER        NOT NULL UNIQUE REFERENCES messages (id),
    text       VARCHAR(10000) NOT NULL
);
CREATE TABLE pic_messages
(
    message_id INTEGER  NOT NULL UNIQUE REFERENCES messages (id),
    pic        BYTEA    NOT NULL,
    type       pic_type NOT NULL,
    caption    VARCHAR(10000)
);
CREATE TABLE audio_messages
(
    message_id INTEGER    NOT NULL UNIQUE REFERENCES messages (id),
    audio      BYTEA      NOT NULL,
    type       audio_type NOT NULL
);
CREATE TABLE video_messages
(
    message_id INTEGER NOT NULL UNIQUE REFERENCES messages (id),
    video      BYTEA   NOT NULL
);
CREATE TABLE doc_messages
(
    message_id INTEGER NOT NULL UNIQUE REFERENCES messages (id),
    doc        BYTEA   NOT NULL
);
CREATE TABLE group_chat_invite_messages
(
    message_id    INTEGER NOT NULL UNIQUE REFERENCES messages (id),
    group_chat_id INTEGER NOT NULL REFERENCES group_chats (id)
);
CREATE TABLE poll_messages
(
    id         SERIAL PRIMARY KEY,
    message_id INTEGER        NOT NULL UNIQUE REFERENCES messages (id),
    title      VARCHAR(10000) NOT NULL
);
CREATE TABLE poll_options
(
    id      SERIAL PRIMARY KEY,
    poll_id INTEGER        NOT NULL REFERENCES poll_messages (id),
    option  VARCHAR(10000) NOT NULL
);
CREATE TABLE poll_votes
(
    user_id   INTEGER NOT NULL REFERENCES users (id),
    option_id INTEGER NOT NULL REFERENCES poll_options (id)
);
CREATE TABLE contacts
(
    id               SERIAL PRIMARY KEY,
    contact_owner_id INTEGER NOT NULL REFERENCES users (id),
    contact_id       INTEGER NOT NULL REFERENCES users (id)
);
CREATE TABLE group_chat_users
(
    id            SERIAL PRIMARY KEY,
    group_chat_id INTEGER NOT NULL REFERENCES group_chats (id),
    user_id       INTEGER NOT NULL REFERENCES users (id),
    is_admin      BOOLEAN NOT NULL
);
CREATE TABLE private_chats
(
    id        INTEGER NOT NULL UNIQUE REFERENCES chats (id),
    user_1_id INTEGER NOT NULL REFERENCES users (id),
    user_2_id INTEGER NOT NULL REFERENCES users (id)
);
CREATE TABLE private_chat_deletions
(
    id        SERIAL PRIMARY KEY,
    chat_id   INTEGER   NOT NULL REFERENCES private_chats (id),
    date_time TIMESTAMP NOT NULL,
    user_id   INTEGER   NOT NULL REFERENCES users (id)
);
CREATE TABLE stargazers
(
    user_id    INTEGER NOT NULL REFERENCES users (id),
    message_id INTEGER NOT NULL REFERENCES messages (id)
);
CREATE TABLE message_statuses
(
    message_id INTEGER        NOT NULL REFERENCES messages (id),
    status     message_status NOT NULL,
    user_id    INTEGER        NOT NULL REFERENCES users (id),
    date_time  TIMESTAMP      NOT NULL
);
CREATE TABLE typing_statuses
(
    chat_id   INTEGER NOT NULL REFERENCES chats (id),
    user_id   INTEGER NOT NULL REFERENCES users (id),
    is_typing BOOLEAN NOT NULL
);
CREATE TABLE action_messages
(
    id         SERIAL PRIMARY KEY,
    message_id INTEGER        NOT NULL UNIQUE REFERENCES messages (id),
    text       VARCHAR(10000) NOT NULL
);
CREATE TABLE action_message_actions
(
    action_message_id INTEGER        NOT NULL REFERENCES action_messages (id),
    action            VARCHAR(10000) NOT NULL
);
