package models

import dtos.{NewUser, UserWithoutPassword}
import org.joda.time.DateTime

case class User(
    id: String,
    username: String,
    password: String,
    email: String,
    firstName: String,
    lastName: String,
    birthDate: DateTime,
    deleted: Boolean
)

object User {
  def apply(id: String, user: NewUser): User =
    User(
      id = id,
      username = user.username,
      password = user.password,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      birthDate = user.birthDate,
      deleted = false
    )

  implicit class UserOps(user: User) {
    def toUserWithoutPassword: UserWithoutPassword =
      UserWithoutPassword(
        id = user.id,
        username = user.username,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        birthDate = user.birthDate,
        deleted = user.deleted
      )
  }
}
