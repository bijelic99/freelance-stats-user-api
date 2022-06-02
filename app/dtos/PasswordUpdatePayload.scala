package dtos

case class PasswordUpdatePayload(
    oldPassword: String,
    newPassword: String
)
