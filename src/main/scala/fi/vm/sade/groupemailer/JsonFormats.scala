package fi.vm.sade.groupemailer

import fi.vm.sade.utils.json4s.GenericJsonFormats
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, EntityEncoder, MediaType}
import org.json4s._
import org.json4s.jackson.Serialization

trait JsonFormats {
  implicit val jsonFormats: Formats = GenericJsonFormats.genericFormats
}

object Json4sHttp4s {
  def json4sEncoderOf[A <: AnyRef](implicit formats: Formats, mf: Manifest[A]): EntityEncoder[A] = {
    EntityEncoder.stringEncoder(Charset.`UTF-8`).contramap[A](item => Serialization.write[A](item))
      .withContentType(`Content-Type`(MediaType.`application/json`))
  }
}
