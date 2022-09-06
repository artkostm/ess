package io.github.artkostm.t6

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all.*
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import io.circe.*
import io.circe.parser.*
import io.github.artkostm.data.DataProvider
import io.github.artkostm.es.Client
import io.github.artkostm.index.Indexer
import io.github.artkostm.search.SearchSampler
import io.github.artkostm.t6.Cli.{CliParams, TransportParams}
import io.github.artkostm.tablefy.{Box, table}
import org.legogroup.woof.{*, given}

trait Application[F[_]]:
  def run(params: TransportParams): F[Unit]
  def search(params: CliParams): F[Unit]

object Application:
  def make[F[_]: Logger: DataProvider: Client: Indexer: SearchSampler]: F[Application[F]] =
    MonadThrow[F].pure(
      new Application[F]:
        override def run(params: TransportParams): F[Unit] =
          for
            _                 <- Logger[F].info(s"Starting indexing app with the following params: $params")
            dataStream        <- DataProvider[F].read()
            indexName          = params.index.getOrElse(utils.getFileName(params.file))
            _                 <- Client[F].create(indexName)
            (indexed, errors) <- Indexer[F].indexData(indexName, dataStream, params.id)
            _                 <- Logger[F].info(s"Indexing completed.")
            _                 <- Logger[F].info(s"Successfully indexed $indexed rows. Errors: $errors")
          yield ()

        override def search(params: CliParams): F[Unit] =
          for
            _ <- Logger[F].info(s"Starting search app with the following params: $params")
            _ <- SearchSampler[F].run(params.file)
            _ <- Logger[F].info(s"Searching completed.")
          yield ()
    )
