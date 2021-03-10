package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.Users
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

private val session: Session = run {
    val properties = Properties()
    properties["mail.smtp.host"] = System.getenv("SMTP_HOST")
    properties["mail.smtp.port"] = System.getenv("SMTP_TLS_PORT")
    properties["mail.smtp.starttls.enable"] = true
    properties["mail.smtp.auth"] = true
    Session.getInstance(properties, EmailAuthenticator)
}

private object EmailAuthenticator : Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication =
        PasswordAuthentication(System.getenv("SMTP_USERNAME"), System.getenv("SMTP_PASSWORD"))
}

/**
 * Sends an email to the [emailAddress] with a verification [code] which is to be passed to the GraphQL mutation
 * `verifyEmailAddress`. Use [emailEmailAddressVerification] if the [emailAddress] is registered.
 */
fun emailNewEmailAddressVerification(emailAddress: String, code: Int) {
    emailEmailAddressVerification(emailAddress, code)
}

/**
 * Sends an email to the [emailAddress] with a verification code which is to be passed to the GraphQL mutation
 * `verifyEmailAddress`. Use [emailNewEmailAddressVerification] if the [emailAddress] is unregistered.
 */
fun emailEmailAddressVerification(emailAddress: String) {
    val code = Users.read(emailAddress).emailAddressVerificationCode
    emailEmailAddressVerification(emailAddress, code)
}

private fun emailEmailAddressVerification(emailAddress: String, code: Int) {
    val name = System.getenv("APP_NAME")
    email(
        emailAddress,
        subject = "Verify your $name account's email address",
        body = "Please enter your verification code, $code, in $name to verify your email address.",
    )
}

fun emailResetPassword(emailAddress: String) {
    val code = Users.read(emailAddress).passwordResetCode
    val name = System.getenv("APP_NAME")
    email(
        emailAddress,
        subject = "Reset your $name account's password",
        body = "Please enter your verification code, $code, in $name to reset your password.",
    )
}

/** Sends an email [to] the specified address having the [subject] and [body]. */
private fun email(to: String, subject: String, body: String) {
    val message = MimeMessage(session)
    message.setFrom(InternetAddress(System.getenv("SMTP_FROM")))
    message.setRecipients(Message.RecipientType.TO, to)
    message.subject = subject
    message.setText(body)
    Transport.send(message)
}

/** Whether the [emailAddress]'s domain (e.g., `"example.com"`) is allowed by this Omni Chat instance. */
fun hasAllowedDomain(emailAddress: String): Boolean {
    val domains = System.getenv("ALLOWED_EMAIL_DOMAINS")
    if (domains.isEmpty()) return true
    return emailAddress.substringAfter("@") in domains.split(",")
}
