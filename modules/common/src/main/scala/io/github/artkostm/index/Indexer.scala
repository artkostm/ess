package io.github.artkostm.index

import cats.syntax.all.*
import cats.effect.{Async, MonadCancelThrow}
import io.github.artkostm.es.Client
import org.legogroup.woof.{*, given}

trait Indexer[F[_]: Client]:
  def indexData(index: String, data: fs2.Stream[F, Map[String, String]], id: Option[String]): F[(Long, Long)]

object Indexer:
  def apply[F[_]: Indexer]: Indexer[F] = summon

  def make[F[_]: Client: Logger: MonadCancelThrow: Async](chunkSize: Int = 100): F[Indexer[F]] =
    Async[F].pure(
      new Indexer[F]:
        override def indexData(index: String, data: fs2.Stream[F, Map[String, String]], id: Option[String]): F[(Long, Long)] =
          for
            _ <- Logger[F].info("Starting indexing data...")
            stats <- data
                   .chunkN(chunkSize, allowFewer = true)
                   .evalMap { chunk =>
                     MonadCancelThrow[F].handleErrorWith(Client[F].indexBulk(index, chunk.toList, id)) { error =>
                       Logger[F].error(s"Error while indexing data chunk: \n$error") *> Async[F].pure((0, chunk.size))
                     }
                   }
                   .compile
                   .fold((0L, 0L)) {
                     case ((succ, fail), (chunkSucc, chunkFail)) => (succ + chunkSucc, fail + chunkFail)
                   }
          yield stats
    )
