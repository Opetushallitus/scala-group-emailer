package fi.sade.vm.groupemailer

trait GroupEmailService {
  def send(email: GroupEmail): Option[String]
}