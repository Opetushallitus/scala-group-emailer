package fi.sade.vm.groupemailer

case class GroupEmail(recipient: List[Recipient], email: EmailInfo)
case class Recipient(oid: Option[String], email: String, languageCode: String, recipientReplacements: List[Replacement], oidType: Option[String] = Some("opiskelija"))
case class EmailInfo(callingProcess: String, templateName: String)
case class Replacement(name: String, value: String)
