package configurations

import pdi.jwt.JwtAlgorithm

class JwtServiceConfiguration {
  val key = "secretKey"
  val algo = JwtAlgorithm.fromString("HS256")
}
