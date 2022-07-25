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
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}
import org.legogroup.woof.{*, given}

trait Client[F[_]]:
  def create(index: String): F[Unit]
  def indexBulk(index: String, batch: Seq[Map[String, String]], id: Option[String]): F[(Int, Int)]

object Client:
  inline def apply[F[_]: Client]: Client[F] = summon

  def java[F[_]: Executor: Functor: Logger: Async](config: ClientConfig): Resource[F, Client[F]] =
    Resource.fromAutoCloseable[F, ElasticClient](Async[F].pure(ElasticClient(createJavaClient(config)))).map { es =>
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
    }

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

final case class ClientConfig(
    url: String,
    username: Option[String],
    password: Option[Array[Byte]],
    connectionTimeout: Int = 2 * 60 * 1000,
    socketTimeout: Int = 2 * 60 * 1000
)
