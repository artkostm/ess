package io.github.artkostm

import cats.syntax.all.*
import cats.effect.Async
import io.github.artkostm.Cli.CliParams
import io.github.artkostm.data.DataProvider
import io.github.artkostm.es.Client
import io.github.artkostm.index.Indexer
import org.legogroup.woof.{*, given}

trait Application[F[_]]:
  def run(params: CliParams): F[Unit]

object Application:
  def make[F[_]: Logger: DataProvider: Client: Indexer: Async]: F[Application[F]] =
    Async[F].pure(
      new Application[F]:
        override def run(params: CliParams): F[Unit] =
          for
            _                <- Logger[F].info(s"Starting indexing app with the following params: $params")
            dataStream       <- DataProvider[F].read()
            indexName         = params.index.getOrElse(utils.getFileName(params.file))
            _                <- Client[F].create(indexName)
            stats            <- Indexer[F].indexData(indexName, dataStream, params.id)
            (indexed, errors) = stats
            _                <- Logger[F].info(s"Indexing completed.")
            _                <- Logger[F].info(s"Successfully indexed $indexed rows. Errors: $errors")
          yield ()
    )
