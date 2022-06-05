package repositories

import com.google.inject.ImplementedBy
import dtos.Credentials
import models.User
import play.api.libs.json.JsObject

import scala.concurrent.Future

@ImplementedBy(classOf[MongoUserRepository])
trait UserRepository {
  def get(userId: String): Future[Option[User]]

  def get(credentials: Credentials): Future[Option[User]]

  def add(user: User): Future[User]

  def update(
      userId: String,
      fields: JsObject,
      fetchNewObject: Boolean
  ): Future[Option[User]]
}
