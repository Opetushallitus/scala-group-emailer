package fi.vm.sade.groupemailer

import fi.vm.sade.utils.json4s.GenericJsonFormats
import org.json4s._

trait JsonFormats {
  implicit val jsonFormats: Formats = GenericJsonFormats.genericFormats
}