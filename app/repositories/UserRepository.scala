package repositories

import dtos.{Credentials, NewUser, UserWithoutPassword}

import scala.concurrent.Future

trait UserRepository {
  def get(userId: String): Future[Option[UserWithoutPassword]]

  def get(credentials: Credentials): Future[Option[UserWithoutPassword]]

  def add(user: NewUser): Future[UserWithoutPassword]

  def update(user: UserWithoutPassword): Future[UserWithoutPassword]
}
