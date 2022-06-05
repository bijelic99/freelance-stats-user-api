package repositories

import dtos.{Credentials, UserWithoutPassword}
import models.User
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.json2bson._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MongoUserRepository @Inject() (
    reactiveMongoApi: ReactiveMongoApi
)(
                                    implicit executionContext: ExecutionContext
) extends UserRepository {
  import utils.PlayJsonFormats._

  def usersCollection: Future[BSONCollection] = reactiveMongoApi.database.map(_.collection("users"))

  override def get(userId: String): Future[Option[User]] = for {
    users <- usersCollection
    maybeUser <- users.find(Json.obj("id" -> userId)).one[User]
  } yield maybeUser

  override def get(credentials: Credentials): Future[Option[User]] = for {
    users <- usersCollection
    maybeUser <- users.find(Json.obj("username" -> credentials.username, "password" -> credentials.password)).one[User]
  } yield maybeUser

  override def add(user: User): Future[User] =
    usersCollection
      .flatMap(_.insert(ordered = false).one(user))
      .map {
        case result if result.writeErrors.isEmpty =>
          user
        case result =>
          throw new Exception(s"Unexpected errors while adding user to the database: '${result.writeErrors.map(_.errmsg).mkString(",\n")}'")
      }

  override def update(user: UserWithoutPassword): Future[User] = for {
    users <- usersCollection
    maybeUser <- users.findAndUpdate(Json.obj("id" -> user.id), Json.obj("$set" -> user), fetchNewObject = true, upsert = false).map(_.value.get.asOpt[User].get)
  } yield maybeUser

}
