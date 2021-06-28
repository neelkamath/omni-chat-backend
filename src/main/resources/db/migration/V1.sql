CREATE
EXTENSION pgcrypto;
CREATE TYPE message_type AS ENUM ('text', 'action', 'pic', 'audio', 'video', 'doc', 'poll', 'group_chat_invite');
CREATE TYPE group_chat_publicity AS ENUM ('not_invitable', 'invitable', 'public');
CREATE TABLE chats
(
    id SERIAL PRIMARY KEY
);
CREATE TABLE pics
(
    id        SERIAL PRIMARY KEY,
    original  BYTEA    NOT NULL,
    thumbnail BYTEA    NOT NULL
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
    original   BYTEA    NOT NULL,
    thumbnail  BYTEA    NOT NULL,
    caption    VARCHAR(10000)
);
CREATE TABLE audio_messages
(
    message_id INTEGER    NOT NULL UNIQUE REFERENCES messages (id),
    audio      BYTEA      NOT NULL
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
    message_id INTEGER        NOT NULL UNIQUE REFERENCES messages (id),
    question VARCHAR(10000) NOT NULL
);
CREATE TABLE poll_message_options
(
    id         SERIAL PRIMARY KEY,
    message_id INTEGER        NOT NULL REFERENCES messages (id),
    option     VARCHAR(10000) NOT NULL
);
CREATE TABLE poll_message_votes
(
    user_id   INTEGER NOT NULL REFERENCES users (id),
    option_id INTEGER NOT NULL REFERENCES poll_message_options (id)
);
CREATE TABLE contacts
(
    contact_owner_user_id INTEGER NOT NULL REFERENCES users (id),
    contact_user_id       INTEGER NOT NULL REFERENCES users (id)
);
CREATE TABLE group_chat_users
(
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
CREATE TABLE typing_statuses
(
    chat_id   INTEGER NOT NULL REFERENCES chats (id),
    user_id   INTEGER NOT NULL REFERENCES users (id)
);
CREATE TABLE action_messages
(
    message_id INTEGER        NOT NULL UNIQUE REFERENCES messages (id),
    text       VARCHAR(10000) NOT NULL
);
CREATE TABLE action_message_actions
(
    id         SERIAL PRIMARY KEY,
    message_id INTEGER        NOT NULL REFERENCES messages (id),
    action     VARCHAR(10000) NOT NULL
);
CREATE TABLE blocked_users
(
    id              SERIAL PRIMARY KEY,
    blocker_user_id INTEGER NOT NULL REFERENCES users (id),
    blocked_user_id INTEGER NOT NULL REFERENCES users (id)
);
