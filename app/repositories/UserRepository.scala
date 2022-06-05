package repositories

import dtos.{Credentials, NewUser, UserWithoutPassword}
import models.User

import scala.concurrent.Future

trait UserRepository {
  def get(userId: String): Future[Option[User]]

  def get(credentials: Credentials): Future[Option[User]]

  def add(user: User): Future[User]

  def update(user: UserWithoutPassword): Future[User]
}
