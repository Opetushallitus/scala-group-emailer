package fi.vm.sade.groupemailer

trait Content {
  def batchSize: Int
}

case class GroupEmail(recipient: List[Recipient], email: EmailInfo) extends Content {
  def batchSize = recipient.length
}
case class Recipient(oid: Option[String], email: String, languageCode: String, recipientReplacements: List[Replacement], oidType: Option[String] = Some("opiskelija"))
case class EmailInfo(callingProcess: String, templateName: String, languageCode: String)
case class Replacement(name: String, value: Any)

case class EmailData(email: EmailMessage, recipient: List[EmailRecipient]) extends Content {
  def batchSize = recipient.length
}
case class EmailMessage(callingProcess: String, subject: String, body: String, html: Boolean = true)
case class EmailRecipient(email: String)