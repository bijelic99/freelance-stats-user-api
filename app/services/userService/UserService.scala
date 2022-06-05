package services.userService

import dtos.{Credentials, NewUser, PasswordUpdatePayload, UserWithoutPassword}
import models.User
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}
import repositories.UserRepository

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {
  import utils.PlayJsonFormats._
  import User._

  private val log: Logger = LoggerFactory.getLogger(classOf[UserService])

  def get(userId: String): Future[Option[UserWithoutPassword]] = {
    log.debug(s"Getting user with id of: '${userId}'")
    userRepository
      .get(userId)
      .map(_.map(_.toUserWithoutPassword))
  }

  // TODO Return jwt as well
  def login(credentials: Credentials): Future[Option[UserWithoutPassword]] = {
    log.debug(s"Logging in user with username of: '${credentials.username}'")
    userRepository
      .get(credentials)
      .map(_.map(_.toUserWithoutPassword))
  }

  // TODO implement necessary checks
  def register(user: NewUser): Future[UserWithoutPassword] = {
    log.debug(s"Registering user with username of: '${user.username}'")
    userRepository
      .add(User.apply(UUID.randomUUID().toString, user))
      .map(_.toUserWithoutPassword)
  }

  //  TODO Make sure to not allow to set deleted to true
  def update(user: UserWithoutPassword): Future[UserWithoutPassword] = {
    log.debug(s"Updating user with id of: '${user.id}'")
    userRepository
      .update(user.id, Json.toJson(user).as[JsObject], fetchNewObject = true)
      .map(_.get.toUserWithoutPassword)
  }

  // TODO implement password hashing
  def updatePassword(
      userId: String,
      payload: PasswordUpdatePayload
  ): Future[Unit] = {
    log.debug(s"Updating password with user withId of: '$userId'")
    userRepository
      .update(
        userId,
        Json.obj("password" -> payload.newPassword),
        fetchNewObject = false
      )
      .map(_ => ())
  }

  def delete(userId: String): Future[Unit] = {
    log.debug(s"Deleting user with id of: '$userId'")
    userRepository
      .update(userId, Json.obj("deleted" -> true), fetchNewObject = false)
      .map(_ => ())
  }
}
