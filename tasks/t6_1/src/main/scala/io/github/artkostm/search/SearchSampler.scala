package io.github.artkostm.search

import cats.syntax.all.*
import cats.effect.{MonadCancelThrow, Resource, Sync}
import io.github.artkostm.es.Client
import org.legogroup.woof.{*, given}
import io.circe.*
import io.circe.parser.*
import io.github.artkostm.es.model
import io.github.artkostm.tablefy.{Box, table}

import scala.io.Source
import java.nio.charset.StandardCharsets

trait SearchSampler[F[_]: Client]:
  def run(requestFilePath: String): F[Unit]

object SearchSampler:
  def apply[F[_]: SearchSampler]: SearchSampler[F] = summon

  def make[F[_]: Client: Logger: MonadCancelThrow: Sync](): F[SearchSampler[F]] =
    Sync[F].pure(
      new SearchSampler[F]:
        override def run(requestFilePath: String): F[Unit] =
          Resource
            .fromAutoCloseable[F, Source](Source.fromFile(requestFilePath, StandardCharsets.UTF_8.name()).pure[F])
            .use { requestFileBuffer =>
              for
                requestStr    <- requestFileBuffer.getLines().mkString("\n").pure[F]
                searchRequest <- MonadCancelThrow[F].fromEither(
                                   decodeAccumulating[model.SearchRequest](requestStr).toEither.left.map(v =>
                                     new RuntimeException("Something went wrong while parsing the request: " + v)
                                   )
                                 )
                _             <- Logger[F].info(s"Executing a serch request from the file $requestFilePath")
                resp          <- Client[F].search(searchRequest)
                sample        = Box.render(
                                   table(
                                     resp match
                                       case Nil     => List(List("No data :("))
                                       case x :: xs =>
                                         List(
                                           x.keys.toList,
                                           x.values.toList
                                         ) ::: xs.map(_.values.toList)
                                   )
                                 )
                _             <- Logger[F].info(s"\n$sample\n")
              yield ()
            }
    )
