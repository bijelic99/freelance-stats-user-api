package configurations

import play.api.Configuration

import javax.inject.Inject

class ElasticConfiguration @Inject() (
    configuration: Configuration
) {

  val endpoint: String = configuration.get[String]("elastic.endpoint")

  val userIndex: String = configuration.get[String]("elastic.userIndex")

  val userReindexBatchSize: Int = configuration
    .getOptional[Int]("elastic.userReindexBatchSize")
    .getOrElse(10)

}
