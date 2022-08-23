package services

import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import com.freelanceStats.jwtAuth.models.JwtToken
import com.freelanceStats.jwtAuth.services.JwtService
import dtos.{Credentials, NewUser, PasswordUpdatePayload}
import exceptions.ApplicationException
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
    jwtService: JwtService,
    userIndexService: UserIndexService
)(implicit ec: ExecutionContext) {
  import User._
  import utils.PlayJsonFormats._

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
    for {
      existingUser <- userRepository.getByUsername(user.username)
      _ = if (existingUser.isDefined)
        throw ApplicationException.UserAlreadyExists
      else ()
      dbUser <- userRepository.add(
        User.apply(
          UUID.randomUUID().toString,
          user
            .copy(password = passwordService.hashPassword(user.password))
        )
      )
      indexedUser <- userIndexService.indexUser(dbUser.toUserWithoutPassword)
    } yield indexedUser
  }

  def update(user: UserWithoutPassword): Future[UserWithoutPassword] = {
    log.debug(s"Updating user with id of: '${user.id}'")
    if (user.deleted) {
      Future.failed(ApplicationException.CantDeleteViaUpdate)
    } else {
      for {
        dbUser <- userRepository
          .update(
            user.id,
            Json.toJson(user).as[JsObject],
            fetchNewObject = true
          )
        userWithoutPassword = dbUser.get.toUserWithoutPassword
        indexedUser <- userIndexService.indexUser(userWithoutPassword)
      } yield indexedUser
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
    for {
      userOpt <- userRepository.get(userId)
      if userOpt.isDefined
      user = userOpt.get
      _ <- userRepository.update(
        userId,
        Json.obj("deleted" -> true),
        fetchNewObject = false
      )
      _ <- userIndexService.indexUser(
        user.copy(deleted = true).toUserWithoutPassword
      )
    } yield ()
  }

  def checkIfUsernameExists(username: String): Future[Boolean] =
    userRepository
      .getByUsername(username)
      .map(_.isDefined)
}
