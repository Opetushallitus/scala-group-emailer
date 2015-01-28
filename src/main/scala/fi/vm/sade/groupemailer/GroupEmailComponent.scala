package fi.vm.sade.groupemailer

import java.util.concurrent.atomic.AtomicReference

import fi.vm.sade.utils.cas.{CasClient, CasConfig, CasTicketRequest}
import fi.vm.sade.utils.http.{HttpRequest, DefaultHttpClient}
import fi.vm.sade.utils.slf4j.Logging
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import scalaj.http.HttpOptions

trait GroupEmailComponent {
  val groupEmailService: GroupEmailService

  class RemoteGroupEmailService(groupEmailerSettings: GroupEmailerSettings) extends GroupEmailService with JsonFormats with Logging {
    case class Session(jsessionid: String, created: DateTime)
    private val cachedSession: AtomicReference[Option[Session]] = new AtomicReference[Option[Session]](None)
    private val jsessionPattern = """(^JSESSIONID=[^;]+)""".r
    private lazy val casClient = new CasClient(new CasConfig(groupEmailerSettings.casUrl))
    private val httpOptions = Seq(HttpOptions.connTimeout(10000), HttpOptions.readTimeout(90000))
    private val SessionTimeout: Int = 12

    override def send(groupEmail: GroupEmail): Option[String] = {
      sendJson(groupEmail)
    }

    override def sendMailWithoutTemplate(htmlEmail: EmailData): Option[String] = {
      sendJson(htmlEmail)
    }

    private def sendJson(content: Content) = {
      withSession {
        case Some(session) => {
          val groupEmailRequest = DefaultHttpClient.httpPost(groupEmailerSettings.groupEmailServiceUrl, Some(Serialization.write(content)), httpOptions: _*)
            .header("Cookie", session.jsessionid)
            .header("Content-type", "application/json")

          logger.info(s"Sending ${content.batchSize} emails to ${groupEmailerSettings.groupEmailServiceUrl}")
          groupEmailRequest.responseWithHeaders() match {
            case (status, _, body) if status >= 200 && status < 300 => {
              val jobId = (parse(body) \ "id").extractOpt[String]
              logger.info(s"Group email sent successfully, jobId: $jobId")
              jobId
            }
            case (status, _, body) => {
              cachedSession.set(None)
              throw new IllegalStateException(s"Group smail sending failed to ${groupEmailerSettings.groupEmailServiceUrl}. Response status was: $status. Server replied: $body")
            }
          }
        }
        case _ => {
          cachedSession.set(None)
          throw new IllegalStateException("Group email sending failed. Failed to get a CAS session going.")
        }
      }
    }

    private def withSession(block: Option[Session] => Option[String]) = {
      cachedSession.get() match {
        case Some(session) => {
          if (session.created.plusHours(SessionTimeout).isBeforeNow) cachedSession.set(requestSession)
        }
        case _ => cachedSession.set(requestSession)
      }
      block(cachedSession.get())
    }

    private def requestSession: Option[Session] = {
      val casTicket = casClient.getServiceTicket(
        new CasTicketRequest(groupEmailerSettings.groupEmailCasUrl, groupEmailerSettings.groupEmailCasUsername, groupEmailerSettings.groupEmailCasPassword)
      )

      casTicket match {
        case Some(ticket) => {
          val sessionRequest = DefaultHttpClient.httpGet(groupEmailerSettings.groupEmailSessionUrl).param("ticket", ticket)
          for {
            jsessionidCookie <- extractSetCookieHeader(sessionRequest) if jsessionidCookie.startsWith("JSESSIONID")
            cookieString <- jsessionPattern.findFirstIn(jsessionidCookie)
          } yield Session(cookieString, new DateTime)
        }
        case _ =>
          logger.error("Could not get CAS service ticket")
          None
      }
    }

    def extractSetCookieHeader(sessionRequest: HttpRequest): Option[String] = {
      sessionRequest.responseWithHeaders() match {
        case (status, headers, _) if status >= 200 && status < 300 => headers.get("Set-Cookie")
        case (status, _, body) => {
          logger.error(s"Failed session request to ${groupEmailerSettings.groupEmailSessionUrl}. Response status was: $status. Server replied: $body")
          None
        }
      }
    }
  }

  class FakeGroupEmailService extends GroupEmailService with Logging with JsonFormats {
    private var lastEmailSize = 0
    def getLastEmailSize = lastEmailSize
    override def send(email: GroupEmail): Option[String] = {
      logger.info(s"send GroupEmail: ${Serialization.write(email)}")
      lastEmailSize = email.recipient.size
      Some("Thank you for using fake group email service")
    }

    override def sendMailWithoutTemplate(htmlEmail: EmailData) = {
      logger.info(s"sendMailWithoutTemplate EmailData: ${Serialization.write(htmlEmail)}")
      Some(Serialization.write(htmlEmail))
    }
  }
}