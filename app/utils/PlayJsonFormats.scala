package utils

import dtos.{Credentials, NewUser, PasswordUpdatePayload}
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import models.{SearchResponse, User}
import play.api.libs.json.{Format, Json, OFormat}

object PlayJsonFormats {
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val credentialsFormat: OFormat[Credentials] =
    Json.format[Credentials]
  implicit val newUserFormat: OFormat[NewUser] = Json.format[NewUser]
  implicit val userWithoutPasswordFormat: Format[UserWithoutPassword] =
    com.freelanceStats.commons.implicits.playJson.ModelsFormat.userFormat
  implicit val passwordUpdatePayloadFormat: OFormat[PasswordUpdatePayload] =
    Json.format[PasswordUpdatePayload]

  implicit val userSearchResponseFormat
      : Format[SearchResponse[UserWithoutPassword]] =
    Json.format[SearchResponse[UserWithoutPassword]]
}
