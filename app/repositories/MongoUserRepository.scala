package repositories

import dtos.Credentials
import models.User
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.json2bson._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MongoUserRepository @Inject() (
    reactiveMongoApi: ReactiveMongoApi
)(implicit
    executionContext: ExecutionContext
) extends UserRepository {
  import utils.PlayJsonFormats._

  def usersCollection: Future[BSONCollection] =
    reactiveMongoApi.database.map(_.collection("users"))

  override def get(userId: String): Future[Option[User]] =
    usersCollection
      .flatMap(_.find(Json.obj("id" -> userId)).one[User])

  override def get(credentials: Credentials): Future[Option[User]] = for {
    users <- usersCollection
    maybeUser <- users
      .find(
        Json.obj(
          "username" -> credentials.username,
          "password" -> credentials.password
        )
      )
      .one[User]
  } yield maybeUser

  override def add(user: User): Future[User] =
    usersCollection
      .flatMap(_.insert.one(user))
      .map {
        case result if result.writeErrors.isEmpty =>
          user
        case result =>
          throw new Exception(
            s"Unexpected errors while adding user to the database: '${result.writeErrors.map(_.errmsg).mkString(",\n")}'"
          )
      }

  override def update(
      userId: String,
      fields: JsObject,
      fetchNewObject: Boolean
  ): Future[Option[User]] =
    usersCollection
      .flatMap(
        _.findAndUpdate(
          Json.obj("id" -> userId),
          Json.obj("$set" -> fields),
          fetchNewObject = fetchNewObject,
          upsert = false
        )
      )
      .map(_.value.map(_.asOpt[User].get))

}
