package repositories

import dtos.{Credentials, NewUser, UserWithoutPassword}
import play.modules.reactivemongo.ReactiveMongoApi

import javax.inject.Inject
import scala.concurrent.Future

class MongoUserRepository @Inject() (
    reactiveMongoApi: ReactiveMongoApi
) extends UserRepository {
  override def get(userId: String): Future[Option[UserWithoutPassword]] = ???

  override def get(
      credentials: Credentials
  ): Future[Option[UserWithoutPassword]] = ???

  override def add(user: NewUser): Future[UserWithoutPassword] = ???

  override def update(user: UserWithoutPassword): Future[UserWithoutPassword] =
    ???
}
