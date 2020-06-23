package com.neelkamath.omniChat.graphql

/** Used whenever the client sends invalid data in a valid GraphQL request. */
sealed class ClientException(message: String) : Exception(message)

object NonexistentUserException : ClientException("NONEXISTENT_USER")

object UnverifiedEmailAddressException : ClientException("UNVERIFIED_EMAIL_ADDRESS")

object IncorrectPasswordException : ClientException("INCORRECT_PASSWORD")

object UsernameTakenException : ClientException("USERNAME_TAKEN")

object UsernameNotLowercaseException : ClientException("USERNAME_NOT_LOWERCASE")

object EmailAddressTakenException : ClientException("EMAIL_ADDRESS_TAKEN")

object InvalidChatIdException : ClientException("INVALID_CHAT_ID")

object MissingNewAdminIdException : ClientException("MISSING_NEW_ADMIN_ID")

object InvalidNewAdminIdException : ClientException("INVALID_NEW_ADMIN_ID")

object UnregisteredEmailAddressException : ClientException("UNREGISTERED_EMAIL_ADDRESS")

object UnauthorizedException : ClientException("UNAUTHORIZED")

object InvalidUserIdException : ClientException("INVALID_USER_ID")

object InvalidTitleLengthException : ClientException("INVALID_TITLE_LENGTH")

object InvalidDescriptionLengthException : ClientException("INVALID_DESCRIPTION_LENGTH")

object ChatExistsException : ClientException("CHAT_EXISTS")

object InvalidContactException : ClientException("INVALID_CONTACT")

object InvalidMessageLengthException : ClientException("INVALID_MESSAGE_LENGTH")

object InvalidMessageIdException : ClientException("INVALID_MESSAGE_ID")

object DuplicateStatusException : ClientException("DUPLICATE_STATUS")