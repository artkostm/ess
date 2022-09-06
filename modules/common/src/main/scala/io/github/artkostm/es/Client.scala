package io.github.artkostm.es

import cats.MonadThrow
import cats.effect.{Async, Resource}
import cats.syntax.all.*
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, Executor, Functor, Response}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.{KeywordField, TextField}
import com.sksamuel.elastic4s.handlers.index.GetIndexResponse
import com.sksamuel.elastic4s.http.{JavaClient, NoOpHttpClientConfigCallback}
import com.sksamuel.elastic4s.requests.bulk.{BulkRequest, BulkResponse}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.{CreateIndexRequest, CreateIndexResponse, GetIndexRequest}
import com.sksamuel.elastic4s.requests.searches.{SearchRequest as EsSearchRequest, SearchResponse}
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import org.legogroup.woof.{*, given}

trait Client[F[_]]:
  def create(index: String): F[Unit]
  def indexBulk(index: String, batch: Seq[Map[String, String]], id: Option[String]): F[(Int, Int)]
  def search(request: model.SearchRequest): F[List[Map[String, String]]]
  def matchQueryExample(index: String): F[List[Map[String, String]]]
  def fuzzyQueryExample(index: String): F[List[Map[String, String]]]
  def top10TagsExample(index: String):  F[List[Map[String, String]]]
  def topRatedExample(index: String): F[List[Map[String, String]]]
  def nestedQueryExample(index: String): F[List[Map[String, String]]]

object Client:
  inline def apply[F[_]: Client]: Client[F] = summon

  def java[F[_]: Executor: Functor: Logger: Async](config: ClientConfig): Resource[F, Client[F]] =
    Resource.fromAutoCloseable[F, ElasticClient](ElasticClient(createJavaClient(config)).pure[F]).map { es =>
      makeClient(es)
    }

  def makeClient[F[_]: Executor: Functor: Logger: Async](es: ElasticClient): Client[F] =
    new Client[F]:
      override def create(index: String): F[Unit] =
        for
          _ <- Logger[F].info(s"Checking the $index index.")
          r <- es.execute[GetIndexRequest, Map[String, GetIndexResponse], F](getIndex(index)).flatMap(checkResponse)
          _ <- if r.status == 404 then
            for
              _ <- es.execute[CreateIndexRequest, CreateIndexResponse, F](
                createIndex(index).shards(1).replicas(0)
              ).flatMap(checkResponse)
              _ <- Logger[F].info(s"Index $index created.")
            yield ()
          else Logger[F].info("Index exists.")
        yield ()

      override def indexBulk(index: String, batch: Seq[Map[String, String]], id: Option[String])
      : F[(Int, Int)] =
        for
          r <- es.execute[BulkRequest, BulkResponse, F](
            id.fold(bulk(batch.map { fields => indexInto(index).fields(fields.toSeq*) })) { idCol =>
              bulk(batch.map { fields => indexInto(index).id(fields(idCol)).fields(fields.toSeq*) })
            }.refresh(RefreshPolicy.WaitFor)
          )
          response <- checkResponse[F, BulkResponse](r)
          result = response.result
        yield (result.successes.size, result.failures.size)

      override def search(request: model.SearchRequest): F[List[Map[String, String]]] =
        for
          r <- es.execute(
            summon[Conversion[model.SearchRequest, EsSearchRequest]](request)
          )
          response <- checkResponse(r)
          result   = response.result
          _        <- Logger[F].info(s"Got ${result.hits.total} hits.")
        yield responseToRows(result)

      override def matchQueryExample(index: String): F[List[Map[String, String]]] =
        makeCall(examples.matchComedyGenre(index))(responseToRows)

      override def fuzzyQueryExample(index: String): F[List[Map[String, String]]] =
        makeCall(examples.fuzzyGenres(index))(responseToRows)

      override def nestedQueryExample(index: String): F[List[Map[String, String]]] =
        makeCall(examples.nestedQueryForTags(index))(responseToRows)

      override def topRatedExample(index: String): F[List[Map[String, String]]] =
        makeCall(examples.topRatedByUser(index))(responseToRows)

      override def top10TagsExample(index: String): F[List[Map[String, String]]] =
        makeCall(examples.top10TagsForTerminator(index)) { r =>
          r.aggregations.getAgg("top_10_tags")
            .flatMap(_.getAgg("top_10_tags_for_Terminator"))
            .flatMap(_.dataAsMap.get("buckets"))
            .map(_.asInstanceOf[List[Map[String, Any]]].map(_.view.mapValues(String.valueOf).toMap))
            .getOrElse(List.empty[Map[String, String]])
        }

      private def makeCall[T](request: EsSearchRequest)(f: SearchResponse => T): F[T] =
        for
          _        <- Logger[F].info(s"Making the following search call: \n" + request.show)
          r        <- es.execute(request)
          response <- checkResponse(r)
          result    = response.result
          _        <- Logger[F].info(s"Got ${result.hits.total} hits.")
        yield f(result)

  private def createJavaClient(config: ClientConfig): JavaClient =
    JavaClient(
      ElasticProperties(config.url),
      (requestConfigBuilder: RequestConfig.Builder) =>
        requestConfigBuilder.setConnectTimeout(config.connectionTimeout).setSocketTimeout(config.socketTimeout),
      (config.username zip config.password).fold[HttpClientConfigCallback](NoOpHttpClientConfigCallback) {
        (user, password) => (httpClientBuilder: HttpAsyncClientBuilder) =>
          val creds = new BasicCredentialsProvider()
          creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, new String(password)))
          httpClientBuilder.setDefaultCredentialsProvider(creds)
      }
    )

  private def checkResponse[F[_]: MonadThrow, U](response: Response[U]): F[Response[U]] =
    if response.isError then MonadThrow[F].raiseError(response.error.asException) else MonadThrow[F].pure(response)

  private def responseToRows(result: SearchResponse): List[Map[String, String]] =
    result.hits.hits.map { h =>
      Map(
        "_index" -> String.valueOf(h.index),
        "_id" -> String.valueOf(h.id),
        "_score" -> String.valueOf(h.score)
      ) ++ h.sourceAsMap.mapValues(String.valueOf)
    }.toList

  object examples:
    import com.sksamuel.elastic4s.ElasticDsl.*

    def matchComedyGenre(index: String): EsSearchRequest =
      search(index).matchQuery("genres", "Comedy")
    def fuzzyGenres(index: String): EsSearchRequest =
      search(index).query(fuzzyQuery("genres", "auction").fuzziness("AUTO"))
    def top10TagsForTerminator(index: String): EsSearchRequest =
      search(index).size(0).query(boolQuery().must(matchPhraseQuery("title", "Terminator")))
        .aggs(
          nestedAggregation("top_10_tags", "tags")
            .subAggregations(termsAgg("top_10_tags_for_Terminator", "tags.tag").size(10))
        )
    def topRatedByUser(index: String): EsSearchRequest =
      search(index).query(boolQuery().must(
        matchQuery("userId", "6285"),
        rangeQuery("rating").gte(5)
      ))

    def nestedQueryForTags(index: String): EsSearchRequest =
      search(index).query(
        nestedQuery("tags", matchQuery("tags.tag", "good"))
      )

final case class ClientConfig(
    url: String,
    username: Option[String],
    password: Option[Array[Byte]],
    connectionTimeout: Int = 2 * 60 * 1000,
    socketTimeout: Int = 2 * 60 * 1000
)
