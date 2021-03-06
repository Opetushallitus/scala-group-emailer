package fi.vm.sade.groupemailer

import com.typesafe.config._

class GroupEmailerSettings(config: Config) extends fi.vm.sade.utils.config.ApplicationSettings(config) {
  val casUrl = config.getString("cas.url")
  val groupEmailCasUrl = config.getString("ryhmasahkoposti.cas.service")
  val groupEmailCasUsername = config.getString("ryhmasahkoposti.cas.username")
  val groupEmailCasPassword = config.getString("ryhmasahkoposti.cas.password")
  val groupEmailServiceUrl = config.getString("ryhmasahkoposti.service.email.url")
  val emailBatchSize = config.getInt("ryhmasahkoposti.service.batch.size")
}
