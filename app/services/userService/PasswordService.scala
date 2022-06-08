package services.userService

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordService {
  def hashPassword(password: String): String =
    BCrypt.withDefaults().hashToString(6, password.toCharArray)

  def validatePassword(
      plainTextPassword: String,
      hashedPassword: String
  ): Boolean =
    BCrypt
      .verifyer()
      .verify(plainTextPassword.toCharArray, hashedPassword.toCharArray)
      .verified
}
