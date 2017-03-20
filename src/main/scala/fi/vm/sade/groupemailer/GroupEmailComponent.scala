package fi.vm.sade.groupemailer

import fi.vm.sade.utils.cas.{CasParams, CasAuthenticatingClient, CasClient}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s._
import org.http4s.client.blaze
import org.http4s.client.Client
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import scalaz.concurrent.Task

trait GroupEmailComponent {
  val groupEmailService: GroupEmailService

  class RemoteGroupEmailService(groupEmailerSettings: GroupEmailerSettings, calledId: String) extends GroupEmailService with JsonFormats with Logging {
    private val blazeHttpClient: Client = blaze.defaultClient
    private val casClient = new CasClient(groupEmailerSettings.casUrl, blazeHttpClient)
    private val casParams = CasParams(groupEmailerSettings.groupEmailCasUrl, groupEmailerSettings.groupEmailCasUsername, groupEmailerSettings.groupEmailCasPassword)
    private val authenticatingClient = new CasAuthenticatingClient(casClient, casParams, blazeHttpClient, "scala-group-emailer")
    private val callerIdHeader = Header("Caller-Id", calledId)
    private val emailServiceUrl = uriFromString(groupEmailerSettings.groupEmailServiceUrl)

    def uriFromString(url: String): Uri = {
      Uri.fromString(url).toOption.get
    }

    override def send(groupEmail: GroupEmail): Option[String] = {
      sendJson(groupEmail, Json4sHttp4s.json4sEncoderOf[GroupEmail])
    }

    override def sendMailWithoutTemplate(htmlEmail: EmailData): Option[String] = {
      sendJson(htmlEmail, Json4sHttp4s.json4sEncoderOf[EmailData])
    }

    type Decode[ResultType] = (Int, String, Request) => ResultType

    private def runHttp[RequestType <: Content, ResultType](request: Request, content: RequestType, encoder: EntityEncoder[RequestType])(decoder: (Int, String, Request) => ResultType): Task[ResultType] = {
      for {
        response <- authenticatingClient.httpClient.toHttpService =<< request.withBody(content)(encoder)
        text <- response.as[String]
      } yield {
        decoder(response.status.code, text, request)
      }
    }

    private def sendJson[RequestType <: Content](content: RequestType, encoder: EntityEncoder[RequestType]): Option[String] = {
      val request: Request = Request(
        method = Method.POST,
        uri = emailServiceUrl,
        headers = Headers(callerIdHeader)
      )
      def post(): Option[String] =
        runHttp[RequestType, Option[String]](request, content, encoder) {
          case (200, resultString: String, _) =>
            val jobId = (parse(resultString) \ "id").extractOpt[String]
            logger.info(s"Group email sent successfully, jobId: $jobId")
            jobId
          case (code, resultString, uri) =>
            throw new IllegalStateException(s"Group email sending failed to ${groupEmailerSettings.groupEmailServiceUrl}. Response status was: $code. Server replied: $resultString")
        }.unsafePerformSync
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