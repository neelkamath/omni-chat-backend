package com.neelkamath.omniChat.graphql

/** Use a subclass of this whenever the client sends invalid data in a valid GraphQL query. */
sealed class ClientException(message: String) : Exception(message)

class NonexistentUserException : ClientException("NONEXISTENT_USER")

class UnverifiedEmailException : ClientException("UNVERIFIED_EMAIL")

class IncorrectPasswordException : ClientException("INCORRECT_PASSWORD")

class UsernameTakenException : ClientException("USERNAME_TAKEN")

class EmailTakenException : ClientException("EMAIL_TAKEN")

class InvalidChatIdException : ClientException("INVALID_CHAT_ID")

class MissingNewAdminIdException : ClientException("MISSING_NEW_ADMIN_ID")

class InvalidNewAdminIdException : ClientException("INVALID_NEW_ADMIN_ID")

class UnregisteredEmailException : ClientException("UNREGISTERED_EMAIL")

class UnauthorizedException : ClientException("UNAUTHORIZED")

class InvalidUserIdException : ClientException("INVALID_USER_ID")

class InvalidTitleLengthException : ClientException("INVALID_TITLE_LENGTH")

class InvalidDescriptionLengthException : ClientException("INVALID_DESCRIPTION_LENGTH")

class ChatExistsException : ClientException("CHAT_EXISTS")

class InvalidContactException : ClientException("INVALID_CONTACT")

class InvalidMessageLengthException : ClientException("INVALID_MESSAGE_LENGTH")