package fi.vm.sade.groupemailer

import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.jackson.Serialization
import org.http4s.{MediaType, Charset, EntityEncoder}
import org.http4s.headers.`Content-Type`

trait JsonFormats {
  // Using GenericJsonFormats.genericFormats causes AbstractMethodError when using this library so the implementation is copied here
  private val genericFormats: Formats =  new DefaultFormats {
    override def dateFormatter = {
      val format = super.dateFormatter
      format.setTimeZone(DefaultFormats.UTC)
      format
    }
  } ++ JodaTimeSerializers.all

  implicit val jsonFormats: Formats = genericFormats
}

object Json4sHttp4s {
  def json4sEncoderOf[A <: AnyRef](implicit formats: Formats, mf: Manifest[A]): EntityEncoder[A] = {
    EntityEncoder.stringEncoder(Charset.`UTF-8`).contramap[A](item => Serialization.write[A](item))
      .withContentType(`Content-Type`(MediaType.`application/json`))
  }
}
