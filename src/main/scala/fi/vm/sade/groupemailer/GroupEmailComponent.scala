package fi.vm.sade.groupemailer

import java.util.concurrent.atomic.AtomicReference

import fi.vm.sade.utils.cas.{CasClient, CasConfig, CasTicketRequest}
import fi.vm.sade.utils.http.DefaultHttpClient
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
      logger.info("sendMailWithoutTemplate="+Serialization.write(htmlEmail))
      sendJson(htmlEmail)
    }

    private def sendJson(content: Content) = {
      withSession {
        case Some(session) => {
          val groupEmailRequest = DefaultHttpClient.httpPost(groupEmailerSettings.groupEmailServiceUrl, Some(Serialization.write(content)), httpOptions: _*)
            .header("Cookie", session.jsessionid)
            .header("Content-type", "application/json")

          logger.info(s"Sending email to ${groupEmailerSettings.groupEmailServiceUrl}")
          groupEmailRequest.response() match {
            case Some(json) => {
              val jobId = (parse(json) \ "id").extractOpt[String]
              logger.info(s"Batch sent successfully, jobId: $jobId")
              jobId
            }
            case _ => {
              throw new IllegalStateException("Batch sending failed. Service failure.")
            }
          }
        }
        case _ => {
          throw new IllegalStateException("Batch sending failed. Failed to get a CAS session going.")
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
        case Some(casTicket) => {
          val sessionRequest = DefaultHttpClient.httpGet(groupEmailerSettings.groupEmailSessionUrl).param("ticket", casTicket)
          val cookie: Option[String] = for {
            setCookieHeader <- {
              val responseWithHeaders: (Int, Map[String, List[String]], String) = sessionRequest.responseWithHeaders()
              responseWithHeaders._2.get("Set-Cookie")
            }
            jsessionidCookie <- setCookieHeader.find(_.startsWith("JSESSIONID"))
            cookieString <- jsessionPattern.findFirstIn(jsessionidCookie)
          } yield cookieString

          cookie match {
            case Some(cookieString) =>
              Some(Session(cookieString, new DateTime))
            case _ =>
              logger.error("JSESSIONID missing")
              None
          }
        }
        case _ =>
          logger.error("Could not get CAS service ticket")
          None
      }
    }
  }

  class FakeGroupEmailService extends GroupEmailService with Logging with JsonFormats {
    private var lastEmailSize = 0
    def getLastEmailSize = lastEmailSize
    override def send(email: GroupEmail): Option[String] = {
      logger.info(s"Sending email: ${Serialization.write(email)}")
      lastEmailSize = email.recipient.size
      Some("Thank you for using fake group email service")
    }

    override def sendMailWithoutTemplate(htmlEmail: EmailData) = {
      logger.info(s"Sending email: ${Serialization.write(htmlEmail)}")
      Some("Thank you for using fake send mail without template service")
    }
  }
}