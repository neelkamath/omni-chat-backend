package com.neelkamath.omniChat.graphql

object UnauthorizedException :
    Exception("The user didn't supply an auth token, supplied an invalid one, or lacks the required permissions.")

/** Used when the client sends invalid data in a valid GraphQL document. */
sealed class GraphQlDocDataException(message: String) : Exception(message)

object NonexistentUserException : GraphQlDocDataException("NONEXISTENT_USER")

object UnverifiedEmailAddressException : GraphQlDocDataException("UNVERIFIED_EMAIL_ADDRESS")

object UsernameTakenException : GraphQlDocDataException("USERNAME_TAKEN")

object IncorrectPasswordException : GraphQlDocDataException("INCORRECT_PASSWORD")

object EmailAddressTakenException : GraphQlDocDataException("EMAIL_ADDRESS_TAKEN")

object InvalidChatIdException : GraphQlDocDataException("INVALID_CHAT_ID")

object InvalidGroupChatUsersException : GraphQlDocDataException("INVALID_GROUP_CHAT_USERS")

object InvalidNewAdminIdException : GraphQlDocDataException("INVALID_NEW_ADMIN_ID")

object InvalidAdminIdException : GraphQlDocDataException("INVALID_ADMIN_ID")

object UnregisteredEmailAddressException : GraphQlDocDataException("UNREGISTERED_EMAIL_ADDRESS")

object InvalidUserIdException : GraphQlDocDataException("INVALID_USER_ID")

object ChatExistsException : GraphQlDocDataException("CHAT_EXISTS")

object InvalidContactException : GraphQlDocDataException("INVALID_CONTACT")

object InvalidMessageIdException : GraphQlDocDataException("INVALID_MESSAGE_ID")

object DuplicateStatusException : GraphQlDocDataException("DUPLICATE_STATUS")

object CannotDeleteAccountException : GraphQlDocDataException("CANNOT_DELETE_ACCOUNT")

object AdminCannotLeaveException : GraphQlDocDataException("ADMIN_CANNOT_LEAVE")