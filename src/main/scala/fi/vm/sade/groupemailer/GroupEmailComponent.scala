package fi.vm.sade.groupemailer

import fi.vm.sade.utils.cas.{CasParams, CasAuthenticatingClient, CasClient}
import org.http4s._
import org.http4s.client.blaze
import org.http4s.client.blaze.BlazeClient
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization

import scalaz.concurrent.Task

trait GroupEmailComponent {
  val groupEmailService: GroupEmailService

  class RemoteGroupEmailService(groupEmailerSettings: GroupEmailerSettings, calledId: String) extends GroupEmailService with JsonFormats with Logging {
    private val blazeHttpClient: BlazeClient = blaze.defaultClient
    private val casClient = new CasClient(groupEmailerSettings.casUrl, blazeHttpClient)
    private val casParams = CasParams(groupEmailerSettings.groupEmailCasUrl, groupEmailerSettings.groupEmailCasUsername, groupEmailerSettings.groupEmailCasPassword)
    private val httpClient = new CasAuthenticatingClient(casClient, casParams, blazeHttpClient)
    private val callerIdHeader = Header("Caller-Id", calledId)
    private val jsonHeader = Header("Content-type", "application/json")
    private val emailServiceUrl = uriFromString(groupEmailerSettings.groupEmailServiceUrl)

    def uriFromString(url: String): Uri = {
      Uri.fromString(url).toOption.get
    }

    override def send(groupEmail: GroupEmail): Option[String] = {
      sendJson(groupEmail)
    }

    override def sendMailWithoutTemplate(htmlEmail: EmailData): Option[String] = {
      sendJson(htmlEmail)
    }

    type Decode[ResultType] = (Int, String, Request) => ResultType

    private def runHttp[ResultType](request: Request, content: Content)(decoder: (Int, String, Request) => ResultType): Task[ResultType] = {
      for {
        response <- httpClient.apply(request.withBody(Serialization.write(content)))
        text <- response.as[String]
      } yield {
        decoder(response.status.code, text, request)
      }
    }

    private def sendJson(content: Content): Option[String] = {
      val request: Request = Request(
        method = Method.POST,
        uri = emailServiceUrl,
        headers = Headers(callerIdHeader, jsonHeader)
      )
      def post(retryCount: Int = 0): Option[String] =
        runHttp[Option[String]](request, content) {
          case (200, resultString, _) =>
            val jobId = (parse(resultString) \ "id").extractOpt[String]
            logger.info(s"Group email sent successfully, jobId: $jobId")
            jobId
          case (status, _, body) if (retryCount == 0) => {
            logger.warn("Session timeout, retry session init")
            post(1)
          }
          case (code, resultString, uri) =>
            throw new IllegalStateException(s"Group email sending failed to ${groupEmailerSettings.groupEmailServiceUrl}. Response status was: $code. Server replied: $resultString")
        }.run
      post()
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