package fi.vm.sade.groupemailer

trait Content

case class GroupEmail(recipient: List[Recipient], email: EmailInfo) extends Content
case class Recipient(oid: Option[String], email: String, languageCode: String, recipientReplacements: List[Replacement], oidType: Option[String] = Some("opiskelija"))
case class EmailInfo(callingProcess: String, templateName: String)
case class Replacement(name: String, value: String)

case class EmailData(email: EmailMessage, recipient: List[EmailRecipient]) extends Content
case class EmailMessage(callingProcess: String, subject: String, body: String, html: Boolean = true)
case class EmailRecipient(email: String)