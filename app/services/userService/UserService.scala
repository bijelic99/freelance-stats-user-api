package services.userService

import dtos.{Credentials, NewUser, PasswordUpdatePayload}
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import exceptions.ApplicationException
import models.Aliases.JwtToken
import models.User
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}
import repositories.UserRepository

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject() (
    userRepository: UserRepository,
    passwordService: PasswordService,
    jwtService: JwtService
)(implicit ec: ExecutionContext) {
  import utils.PlayJsonFormats._
  import User._

  private val log: Logger = LoggerFactory.getLogger(classOf[UserService])

  def get(userId: String): Future[Option[UserWithoutPassword]] = {
    log.debug(s"Getting user with id of: '$userId'")
    userRepository
      .get(userId)
      .map(_.map(_.toUserWithoutPassword))
  }

  def login(
      credentials: Credentials
  ): Future[(UserWithoutPassword, JwtToken)] = {
    log.debug(s"Logging in user with username of: '${credentials.username}'")
    userRepository
      .getByUsername(credentials.username)
      .map {
        case Some(user)
            if passwordService.validatePassword(
              credentials.password,
              user.password
            ) =>
          user.toUserWithoutPassword
        case _ =>
          throw ApplicationException.WrongUsernameOrPassword
      }
      .flatMap(user => jwtService.encodeToken(user).map(user -> _))
  }

  def register(user: NewUser): Future[UserWithoutPassword] = {
    log.debug(s"Registering user with username of: '${user.username}'")
    userRepository
      .getByUsername(user.username)
      .flatMap {
        case Some(_) =>
          Future.failed(ApplicationException.UserAlreadyExists)
        case None =>
          userRepository
            .add(
              User.apply(
                UUID.randomUUID().toString,
                user
                  .copy(password = passwordService.hashPassword(user.password))
              )
            )
            .map(_.toUserWithoutPassword)
      }
  }

  def update(user: UserWithoutPassword): Future[UserWithoutPassword] = {
    log.debug(s"Updating user with id of: '${user.id}'")
    if (user.deleted) {
      Future.failed(ApplicationException.CantDeleteViaUpdate)
    } else {
      userRepository
        .update(user.id, Json.toJson(user).as[JsObject], fetchNewObject = true)
        .map(_.get.toUserWithoutPassword)
    }
  }

  def updatePassword(
      userId: String,
      payload: PasswordUpdatePayload
  ): Future[Unit] = {
    log.debug(s"Updating password with user withId of: '$userId'")
    if (payload.newPassword.equals(payload.oldPassword)) {
      Future.failed(ApplicationException.EitherNewOrOldPasswordIncorrect)
    } else {
      userRepository
        .get(userId)
        .flatMap {
          case Some(user)
              if passwordService.validatePassword(
                payload.oldPassword,
                user.password
              ) =>
            userRepository
              .update(
                userId,
                Json.obj(
                  "password" -> passwordService
                    .hashPassword(payload.newPassword)
                ),
                fetchNewObject = false
              )
              .map(_ => ())
          case Some(_) =>
            Future.failed(ApplicationException.EitherNewOrOldPasswordIncorrect)
          case None =>
            Future.failed(ApplicationException.UserNotFound)
        }
    }
  }

  def delete(userId: String): Future[Unit] = {
    log.debug(s"Deleting user with id of: '$userId'")
    userRepository
      .update(userId, Json.obj("deleted" -> true), fetchNewObject = false)
      .map(_ => ())
  }
}
