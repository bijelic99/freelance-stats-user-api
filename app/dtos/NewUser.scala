package dtos

import org.joda.time.DateTime

case class NewUser(
    username: String,
    password: String,
    email: String,
    firstName: String,
    lastName: String,
    birthDate: DateTime
)
