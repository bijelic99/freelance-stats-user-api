package utils

import dtos.{Credentials, NewUser, PasswordUpdatePayload, UserWithoutPassword}
import models.User
import play.api.libs.json.{Json, OFormat}

object PlayJsonFormats {
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val credentialsFormat: OFormat[Credentials] =
    Json.format[Credentials]
  implicit val newUserFormat: OFormat[NewUser] = Json.format[NewUser]
  implicit val userWithoutPasswordFormat: OFormat[UserWithoutPassword] =
    Json.format[UserWithoutPassword]
  implicit val passwordUpdatePayloadFormat: OFormat[PasswordUpdatePayload] =
    Json.format[PasswordUpdatePayload]
}
