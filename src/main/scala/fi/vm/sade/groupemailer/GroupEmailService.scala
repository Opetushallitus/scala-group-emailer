package fi.vm.sade.groupemailer

trait GroupEmailService {
  def send(email: GroupEmail): Option[String]
  def sendMailWithoutTemplate(email: EmailData): Option[String]
}