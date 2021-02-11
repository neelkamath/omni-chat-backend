package com.neelkamath.omniChat.graphql.routing

object UnauthorizedException :
    Exception("The user didn't supply an auth token, supplied an invalid one, or lacks the required permissions.")

/** Used when the client sends invalid data in a valid GraphQL document. */
sealed class GraphQlDocDataException(message: String) : Exception(message)

object NonexistentUserException : GraphQlDocDataException("NONEXISTENT_USER")

object UnverifiedEmailAddressException : GraphQlDocDataException("UNVERIFIED_EMAIL_ADDRESS")

object EmailAddressVerifiedException : GraphQlDocDataException("EMAIL_ADDRESS_VERIFIED")

object UsernameTakenException : GraphQlDocDataException("USERNAME_TAKEN")

object IncorrectPasswordException : GraphQlDocDataException("INCORRECT_PASSWORD")

object EmailAddressTakenException : GraphQlDocDataException("EMAIL_ADDRESS_TAKEN")

object InvalidChatIdException : GraphQlDocDataException("INVALID_CHAT_ID")

object InvalidAdminIdException : GraphQlDocDataException("INVALID_ADMIN_ID")

object UnregisteredEmailAddressException : GraphQlDocDataException("UNREGISTERED_EMAIL_ADDRESS")

object InvalidUserIdException : GraphQlDocDataException("INVALID_USER_ID")

object InvalidContactException : GraphQlDocDataException("INVALID_CONTACT")

object InvalidMessageIdException : GraphQlDocDataException("INVALID_MESSAGE_ID")

object CannotDeleteAccountException : GraphQlDocDataException("CANNOT_DELETE_ACCOUNT")

object InvalidPollException : GraphQlDocDataException("INVALID_POLL")

object NonexistentOptionException : GraphQlDocDataException("NONEXISTENT_OPTION")

object InvalidInviteCodeException : GraphQlDocDataException("INVALID_INVITE_CODE")

object InvalidInvitedChatException : GraphQlDocDataException("INVALID_INVITED_CHAT")

object InvalidDomainException : GraphQlDocDataException("INVALID_DOMAIN")

object InvalidActionException : GraphQlDocDataException("INVALID_ACTION")
