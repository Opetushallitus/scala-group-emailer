package fi.vm.sade.groupemailer

case class GroupEmail(recipient: List[Recipient], email: EmailInfo)
case class Recipient(oid: Option[String], email: String, languageCode: String, recipientReplacements: List[Replacement], oidType: Option[String] = Some("opiskelija"))
case class EmailInfo(callingProcess: String, templateName: String)
case class Replacement(name: String, value: String)

case class HtmlEmail(emailMessage: EmailMessage)
case class EmailMessage(callingProcess: String, from: String, to: List[EmailRecipient], subject: String, isHtml: Boolean, body: String)
case class EmailRecipient(oid: String, emailAddress: String)

