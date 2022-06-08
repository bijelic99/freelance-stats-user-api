package services.userService

import configurations.JwtServiceConfiguration
import com.freelanceStats.commons.models.{User => UserWithoutPassword}
import models.Aliases
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.libs.json.Json

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JwtService @Inject() (
    configuration: JwtServiceConfiguration
) {
  import utils.PlayJsonFormats._
  implicit val clock: Clock = Clock.systemUTC()

  def encodeToken(
      user: UserWithoutPassword
  )(implicit ec: ExecutionContext): Future[Aliases.JwtToken] = Future {
    val claim = JwtClaim(
      content = Json.toJson(user).toString()
    )
    JwtJson.encode(claim, configuration.key, configuration.algo)
  }

  def decodeToken(
      token: Aliases.JwtToken
  )(implicit ec: ExecutionContext): Future[JwtClaim] = Future {
    JwtJson
      .decode(token, configuration.key, JwtAlgorithm.allHmac())
      .get
  }

}
