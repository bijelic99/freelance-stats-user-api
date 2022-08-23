package controllers

import dtos.{Credentials, NewUser, PasswordUpdatePayload}
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import com.freelanceStats.jwtAuth.actions.JwtAuthActionBuilder
import com.freelanceStats.jwtAuth.models.AuthenticatedRequest
import exceptions.ApplicationException.WrongUsernameOrPassword
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import services.{UserIndexService, UserService}
import utils.ValidationMappings

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

class UserController @Inject() (
    val controllerComponents: ControllerComponents,
    userService: UserService,
    userIndexService: UserIndexService,
    authActionBuilder: JwtAuthActionBuilder
)(implicit
    executionContext: ExecutionContext
) extends BaseController {

  import utils.PlayJsonFormats._
  import com.freelanceStats.jwtAuth.formats.PlayJson._

  val log: Logger = LoggerFactory.getLogger(classOf[UserController])

  def get(id: String): Action[AnyContent] = authActionBuilder.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
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

  def put(id: String): Action[UserWithoutPassword] = authActionBuilder(
    parse.json[UserWithoutPassword]
  ).async { implicit request: AuthenticatedRequest[UserWithoutPassword] =>
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

  def updatePassword(id: String): Action[PasswordUpdatePayload] =
    authActionBuilder(
      parse.json[PasswordUpdatePayload]
    ).async { implicit request: AuthenticatedRequest[PasswordUpdatePayload] =>
      val payload = request.body

      val (_, errors) =
        ValidationMappings.passwordUpdatePayloadValidationMapping
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

  def delete(id: String): Action[AnyContent] = authActionBuilder.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
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
      userService
        .login(request.body)
        .map { case (user, token) =>
          Ok(Json.obj("user" -> user) ++ Json.toJson(token).as[JsObject])
        }
        .recover {
          case WrongUsernameOrPassword =>
            Forbidden
          case _ =>
            BadRequest
        }
  }

  def search(
      term: Option[String],
      size: Int = 10,
      from: Int = 0
  ): Action[AnyContent] = authActionBuilder.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
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

  def reindex(): Action[AnyContent] = authActionBuilder.async {
    implicit request: AuthenticatedRequest[AnyContent] =>
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
