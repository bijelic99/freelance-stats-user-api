package utils

import dtos.{NewUser, PasswordUpdatePayload}
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.Constraints._

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID

object ValidationMappings {

  private val idMapping = uuid.transform(_.toString, UUID.fromString)
  private val usernameMapping = nonEmptyText
  private val passwordMapping =
    nonEmptyText.verifying(minLength(8), maxLength(128))
  private val firstNameMapping = nonEmptyText.verifying(maxLength(128))
  private val lastNameMapping = nonEmptyText.verifying(maxLength(128))
  private val birthDateMapping = localDateTime.transform[DateTime](
    ldt =>
      new DateTime(
        ldt.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli,
        DateTimeZone.getDefault
      ),
    dt =>
      LocalDateTime.ofInstant(
        Instant.ofEpochMilli(dt.getMillis),
        ZoneId.systemDefault()
      )
  )

  val newUserValidationMapping: Mapping[NewUser] =
    mapping(
      "username" -> usernameMapping,
      "password" -> passwordMapping,
      "email" -> email,
      "firstName" -> firstNameMapping,
      "lastName" -> lastNameMapping,
      "birthDate" -> birthDateMapping
    )(NewUser.apply)(NewUser.unapply)

  val userWithoutPasswordValidationMapping: Mapping[UserWithoutPassword] =
    mapping(
      "id" -> idMapping,
      "username" -> usernameMapping,
      "email" -> email,
      "firstName" -> firstNameMapping,
      "lastName" -> lastNameMapping,
      "birthDate" -> birthDateMapping,
      "deleted" -> boolean
    )(UserWithoutPassword.apply)(UserWithoutPassword.unapply)

  val passwordUpdatePayloadValidationMapping: Mapping[PasswordUpdatePayload] =
    mapping(
      "oldPassword" -> nonEmptyText,
      "newPassword" -> passwordMapping
    )(PasswordUpdatePayload.apply)(PasswordUpdatePayload.unapply)
}
