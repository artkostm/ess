package io.github.artkostm.t11

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
import io.github.artkostm.t11.Cli.CliParams
import org.legogroup.woof.{*, given}

trait Application[F[_]]:
  def run(params: CliParams): F[Unit]

object Application:
  def make[F[_]: Logger: Task11: MonadThrow]: F[Application[F]] =
    MonadThrow[F].pure(
      new Application[F]:
        override def run(params: CliParams): F[Unit] =
          for
            _ <- Logger[F].info(s"Starting task 11 app with the following params: $params")
            _ <- Task11[F].run(params.index)
            _ <- Logger[F].info(s"Completed.")
          yield ()
    )
