package com.neelkamath.omniChat.graphql

/** Use a subclass of this whenever the client sends invalid data in a valid GraphQL request. */
sealed class ClientException(message: String) : Exception(message)

class NonexistentUserException : ClientException("NONEXISTENT_USER")

class UnverifiedEmailAddressException : ClientException("UNVERIFIED_EMAIL_ADDRESS")

class IncorrectCredentialsException : ClientException("INCORRECT_CREDENTIALS")

class UsernameTakenException : ClientException("USERNAME_TAKEN")

class UsernameNotLowercaseException : ClientException("USERNAME_NOT_LOWERCASE")

class EmailAddressTakenException : ClientException("EMAIL_ADDRESS_TAKEN")

class InvalidChatIdException : ClientException("INVALID_CHAT_ID")

class MissingNewAdminIdException : ClientException("MISSING_NEW_ADMIN_ID")

class InvalidNewAdminIdException : ClientException("INVALID_NEW_ADMIN_ID")

class UnregisteredEmailAddressException : ClientException("UNREGISTERED_EMAIL_ADDRESS")

class UnauthorizedException : ClientException("UNAUTHORIZED")

class InvalidUserIdException : ClientException("INVALID_USER_ID")

class InvalidTitleLengthException : ClientException("INVALID_TITLE_LENGTH")

class InvalidDescriptionLengthException : ClientException("INVALID_DESCRIPTION_LENGTH")

class ChatExistsException : ClientException("CHAT_EXISTS")

class InvalidContactException : ClientException("INVALID_CONTACT")

class InvalidMessageLengthException : ClientException("INVALID_MESSAGE_LENGTH")

class InvalidMessageIdException : ClientException("INVALID_MESSAGE_ID")

class DuplicateStatusException : ClientException("DUPLICATE_STATUS")