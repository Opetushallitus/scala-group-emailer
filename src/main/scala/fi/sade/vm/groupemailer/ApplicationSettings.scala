package fi.sade.vm.groupemailer

import com.typesafe.config._

case class ApplicationSettings(config: Config) extends fi.vm.sade.utils.config.ApplicationSettings(config) {
  val casUrl = config.getString("cas.url")
  val groupEmailCasUrl = config.getString("ryhmasahkoposti.cas.service")
  val groupEmailCasUsername = config.getString("ryhmasahkoposti.cas.username")
  val groupEmailCasPassword = config.getString("ryhmasahkoposti.cas.password")
  val groupEmailSessionUrl = config.getString("ryhmasahkoposti.service.session.url")
  val groupEmailServiceUrl = config.getString("ryhmasahkoposti.service.email.url")
}

case class ApplicationSettingsParser() extends fi.vm.sade.utils.config.ApplicationSettingsParser[ApplicationSettings] {
  override def parse(config: Config) = ApplicationSettings(config)
}

trait ApplicationSettingsComponent {
  val settings: ApplicationSettings
}