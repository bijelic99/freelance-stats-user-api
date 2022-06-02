package services.userService

import dtos.{Credentials, NewUser, PasswordUpdatePayload, UserWithoutPassword}
import repositories.UserRepository

import javax.inject.Inject
import scala.concurrent.Future

class UserService @Inject() (
    userRepository: UserRepository
) {
  def get(userId: String): Future[Option[UserWithoutPassword]] = ???

  def login(credentials: Credentials): Future[Option[UserWithoutPassword]] = ???

  def register(user: NewUser): Future[UserWithoutPassword] = ???

  def update(user: UserWithoutPassword): Future[UserWithoutPassword] = ???

  def updatePassword(
      userId: String,
      payload: PasswordUpdatePayload
  ): Future[Unit] = ???

  def delete(userId: String): Future[Unit] = ???
}
