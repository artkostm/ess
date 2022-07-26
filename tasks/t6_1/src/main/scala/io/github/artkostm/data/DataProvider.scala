package io.github.artkostm.data

import cats.effect.Async
import fs2.RaiseThrowable
import fs2.io.file.Files
import io.github.artkostm.data.csv.Csv
import org.legogroup.woof.Logger

trait DataProvider[F[_]]:
  def read(): F[fs2.Stream[F, Map[String, String]]]

object DataProvider:
  inline def apply[F[_]]: DataProvider[F] = summon
  
  def csv[F[_]: Logger: Files: RaiseThrowable: Async](filePath: String, includeLineNumber: Boolean = false): F[DataProvider[F]] =
    Csv.make(filePath, includeLineNumber)