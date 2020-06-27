package com.neelkamath.omniChat.graphql

/** Used when the client sends invalid data in a valid GraphQL document. */
sealed class GraphQlDocDataException(message: String) : Exception(message)

object NonexistentUserException : GraphQlDocDataException("NONEXISTENT_USER")

object UnverifiedEmailAddressException : GraphQlDocDataException("UNVERIFIED_EMAIL_ADDRESS")

object IncorrectPasswordException : GraphQlDocDataException("INCORRECT_PASSWORD")

object UsernameTakenException : GraphQlDocDataException("USERNAME_TAKEN")

object UsernameNotLowercaseException : GraphQlDocDataException("USERNAME_NOT_LOWERCASE")

object EmailAddressTakenException : GraphQlDocDataException("EMAIL_ADDRESS_TAKEN")

object InvalidChatIdException : GraphQlDocDataException("INVALID_CHAT_ID")

object MissingNewAdminIdException : GraphQlDocDataException("MISSING_NEW_ADMIN_ID")

object InvalidNewAdminIdException : GraphQlDocDataException("INVALID_NEW_ADMIN_ID")

object UnregisteredEmailAddressException : GraphQlDocDataException("UNREGISTERED_EMAIL_ADDRESS")

object InvalidUserIdException : GraphQlDocDataException("INVALID_USER_ID")

object InvalidTitleLengthException : GraphQlDocDataException("INVALID_TITLE_LENGTH")

object InvalidDescriptionLengthException : GraphQlDocDataException("INVALID_DESCRIPTION_LENGTH")

object ChatExistsException : GraphQlDocDataException("CHAT_EXISTS")

object InvalidContactException : GraphQlDocDataException("INVALID_CONTACT")

object InvalidMessageLengthException : GraphQlDocDataException("INVALID_MESSAGE_LENGTH")

object InvalidMessageIdException : GraphQlDocDataException("INVALID_MESSAGE_ID")

object DuplicateStatusException : GraphQlDocDataException("DUPLICATE_STATUS")

object CannotDeleteAccountException : GraphQlDocDataException("CANNOT_DELETE_ACCOUNT")