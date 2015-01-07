package fi.vm.sade.groupemailer

import fi.vm.sade.utils.cas.{CasTicketRequest, CasClient, CasConfig}
import fi.vm.sade.utils.http.DefaultHttpClient
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization

import scalaj.http.HttpOptions


trait GroupEmailComponent {

  val groupEmailService: GroupEmailService

  class RemoteGroupEmailService(groupEmailerSettings: GroupEmailerSettings) extends GroupEmailService with JsonFormats with Logging {
    private val jsessionPattern = """(^JSESSIONID=[^;]+)""".r
    private lazy val casClient = new CasClient(new CasConfig(groupEmailerSettings.casUrl))
    private val httpOptions = Seq(HttpOptions.connTimeout(10000), HttpOptions.readTimeout(90000))

    def send(email: GroupEmail): Option[String] = {
      sessionRequest match {
        case Some(sessionId) => {
          val groupEmailRequest = DefaultHttpClient.httpPost(groupEmailerSettings.groupEmailServiceUrl, Some(Serialization.write(email)), httpOptions: _*)
            .header("Cookie", sessionId)
            .header("Content-type", "application/json")

          logger.info(s"Sending email to ${groupEmailerSettings.groupEmailServiceUrl}")
          groupEmailRequest.response() match {
            case Some(json) => {
              val jobId = (parse(json) \ "id").extractOpt[String]
              logger.info(s"Batch sent successfully, jobId: $jobId")
              jobId
            }
            case _ => {
              logger.error(s"Batch sending failed. Service failure.")
              None
            }
          }
        }
        case _ => {
          logger.error(s"Batch sending failed. Failed to get a CAS session going.")
          None
        }
      }
    }

    private def sessionRequest: Option[String] = {
      val ticketRequest = casClient.getServiceTicket(
        new CasTicketRequest(groupEmailerSettings.groupEmailCasUrl, groupEmailerSettings.groupEmailCasUsername, groupEmailerSettings.groupEmailCasPassword)
      )

      ticketRequest match {
        case Some(casTicket) => {
          val sessionRequest = DefaultHttpClient.httpGet(groupEmailerSettings.groupEmailSessionUrl).param("ticket", casTicket)
          for {
            setCookieHeader <- {
              val responseWithHeaders: (Int, Map[String, List[String]], String) = sessionRequest.responseWithHeaders()
              responseWithHeaders._2.get("Set-Cookie")
            }
            jsessionidCookie <- setCookieHeader.find(_.startsWith("JSESSIONID"))
            cookieString <- jsessionPattern.findFirstIn(jsessionidCookie)
          } yield cookieString
        }
        case _ => None
      }
    }
  }

  class FakeGroupEmailService extends GroupEmailService with Logging with JsonFormats {
    private var lastEmailSize = 0
    def getLastEmailSize() = lastEmailSize
    override def send(email: GroupEmail): Option[String] = {
      logger.info(s"Sending email: ${Serialization.write(email)}")
      lastEmailSize = email.recipient.size
      Some("Thank you for using fake group email service")
    }
  }
}