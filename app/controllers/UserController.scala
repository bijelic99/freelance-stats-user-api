package controllers

import dtos.{Credentials, NewUser, PasswordUpdatePayload}
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc._
import services.{UserIndexService, UserService}
import utils.ValidationMappings

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    userService: UserService,
    userIndexService: UserIndexService
)(implicit
    executionContext: ExecutionContext
) extends BaseController {

  import utils.PlayJsonFormats._

  val log: Logger = LoggerFactory.getLogger(classOf[UserController])

  def get(id: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      userService
        .get(id)
        .map {
          case Some(user) =>
            Ok(Json.toJson(user))
          case None =>
            NotFound
        }
        .recover { t =>
          log.error(
            s"Request for user with id of: '$id' got unexpected error",
            t
          )
          InternalServerError
        }
  }

  def register(): Action[NewUser] = Action(parse.json[NewUser]).async {
    implicit request: Request[NewUser] =>
      val newUser = request.body

      val (_, errors) = ValidationMappings.newUserValidationMapping
        .unbindAndValidate(newUser)

      if (errors.isEmpty) {
        userService
          .register(newUser)
          .map(Json.toJson(_))
          .map(Created(_))
          .recover { case t =>
            log.error(s"Unexpected error while trying to register the user", t)
            InternalServerError
          }
      } else {
        Future.successful(
          BadRequest(
            Json.obj("errors" -> errors.map(_.message))
          )
        )
      }
  }

  def put(id: String): Action[UserWithoutPassword] = Action(
    parse.json[UserWithoutPassword]
  ).async { implicit request: Request[UserWithoutPassword] =>
    request.body
      .pipe {
        case user if user.id.equals(id) =>
          val (_, errors) =
            ValidationMappings.userWithoutPasswordValidationMapping
              .unbindAndValidate(user)

          if (errors.isEmpty) {
            userService
              .update(user)
              .map(Json.toJson(_))
              .map(Ok(_))
              .recover { case t =>
                log.error(
                  s"Unexpected error while trying to update user with id of: '$id'",
                  t
                )
                InternalServerError
              }
          } else {
            Future.successful(
              BadRequest(
                errors.map(_.message).mkString(",\n")
              )
            )
          }
        case _ =>
          Future.successful(
            BadRequest("Updating id is not allowed")
          )
      }
  }

  def updatePassword(id: String): Action[PasswordUpdatePayload] = Action(
    parse.json[PasswordUpdatePayload]
  ).async { implicit request: Request[PasswordUpdatePayload] =>
    val payload = request.body

    val (_, errors) = ValidationMappings.passwordUpdatePayloadValidationMapping
      .unbindAndValidate(payload)

    if (errors.isEmpty) {
      userService
        .updatePassword(id, payload)
        .map(_ => Ok)
        .recover { case t =>
          log.error(
            s"Unexpected error while trying to update password for user with id: '$id'",
            t
          )
          InternalServerError
        }
    } else {
      Future.successful(
        BadRequest(
          errors.map(_.message).mkString(",\n")
        )
      )
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      userService
        .delete(id)
        .map(_ => Ok)
        .recover { case t =>
          log.error(
            s"Unexpected error while trying to delete user with id of: '$id'",
            t
          )
          InternalServerError
        }
  }

  def login(): Action[Credentials] = Action(parse.json[Credentials]).async {
    implicit request: Request[Credentials] =>
      ???
  }

  def search(
      term: Option[String],
      size: Int = 10,
      from: Int = 0
  ): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      userIndexService
        .searchUsers(term, size, from)
        .map(Json.toJson(_))
        .map(Ok(_))
        .recover { t =>
          val message = "Unexpected error while searching for users"
          log.error(message, t)
          InternalServerError(message)
        }
  }

  def reindex(): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      userIndexService.reindexUsers
        .map(_ => Ok)
        .recover(t => InternalServerError(t.getMessage))
  }

  def checkUsername(username: String): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      userService
        .checkIfUsernameExists(username)
        .map {
          case true =>
            Ok
          case false =>
            NotFound
        }
  }

}
