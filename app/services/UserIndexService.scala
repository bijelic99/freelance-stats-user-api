package services

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import com.freelanceStats.commons.models.User
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess}
import configurations.ElasticConfiguration
import models.SearchResponse
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import repositories.UserRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

class UserIndexService @Inject() (
    client: ElasticClient,
    elasticConfiguration: ElasticConfiguration,
    userRepository: UserRepository
)(implicit
    ec: ExecutionContext,
    system: ActorSystem,
    materializer: Materializer
) {
  import com.sksamuel.elastic4s.ElasticDsl._
  import utils.PlayJsonFormats._

  private val log: Logger =
    LoggerFactory.getLogger(classOf[UserIndexService])

  def indexUser(
      user: User
  ): Future[User] =
    client
      .execute(
        updateById(
          elasticConfiguration.userIndex,
          user.id
        ).docAsUpsert(
          Json.toJson(user).toString()
        )
      )
      .map {
        case RequestSuccess(_, _, _, _) =>
          user
        case RequestFailure(_, _, _, error) =>
          throw new Exception(
            s"Error while indexing user with id of: '${user.id}'",
            error.asException
          )
      }

  def reindexUsers: Future[Unit] =
    userRepository.getAll
      .map(metadata =>
        updateById(elasticConfiguration.userIndex, metadata.id)
          .docAsUpsert(
            Json.toJson(metadata).toString()
          )
      )
      .grouped(elasticConfiguration.userReindexBatchSize)
      .mapAsync(1)(requests => client.execute(bulk(requests)))
      .toMat(
        Sink.foreach {
          case RequestSuccess(_, _, _, result) if !result.hasFailures =>
            log.trace("Batch indexed successfully")
          case RequestSuccess(_, _, _, result) =>
            val failures =
              result.failures.flatMap(_.error).map(_.reason).mkString(",\n")
            val message = s"Error while indexing batch, reasons: $failures"
            log.error(message)
            throw new Exception(message)
          case RequestFailure(_, _, _, error) =>
            throw new Exception("Error while indexing batch", error.asException)
        }
      )(Keep.right)
      .run()
      .map(_ => ())

  def searchQuery(term: String): Query =
    boolQuery()
      .filter(
        termQuery("deleted", false)
      )
      .should(
        termQuery("id", term),
        matchQuery("username", term),
        matchQuery("email", term),
        matchQuery("firstName", term),
        termQuery("lastName", term)
      )
      .minimumShouldMatch(1)

  def searchUsers(
      term: Option[String],
      size: Int,
      from: Int
  ): Future[SearchResponse[User]] =
    client
      .execute(
        search(
          elasticConfiguration.userIndex
        )
          .pipe(x => term.fold(x)(term => x.query(searchQuery(term))))
          .size(size)
          .from(from)
      )
      .map {
        case RequestSuccess(_, _, _, result) =>
          SearchResponse(
            result.hits.hits.toSeq
              .map(hit => Json.parse(hit.sourceAsString).as[User]),
            result.totalHits
          )
        case RequestFailure(_, _, _, error) =>
          throw new Exception(
            "Error while searching for data",
            error.asException
          )
      }

}
