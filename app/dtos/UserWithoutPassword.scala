package dtos

import org.joda.time.DateTime

case class UserWithoutPassword(
    id: String,
    username: String,
    email: String,
    firstName: String,
    lastName: String,
    birthDate: DateTime,
    deleted: Boolean
)
