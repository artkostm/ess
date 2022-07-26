package io.github.artkostm.data.csv

import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.data.csv.*
import fs2.io.file.{Files, Flags, Path}
import org.legogroup.woof.{*, given}

trait Csv[F[_]] extends DataProvider[F]

object Csv:
  val LineNumberColumn = "line_number"
  inline def apply[F[_]: Csv]: Csv[F] = summon

  def make[F[_]: Logger: Files: RaiseThrowable: Async](filePath: String, includeLineNumber: Boolean = false): F[Csv[F]] =
    Async[F].pure(
      new Csv[F]:
        override def read(): F[Stream[F, Map[String, String]]] =
          for 
            _ <- Logger[F].info(s"Reading data from $filePath...")
          yield
            Files[F]
              .readAll(Path(filePath), 1024, Flags.Read)
              .through(text.utf8.decode)
              .through(lowlevel.rows())
              .through(lowlevel.headers[F, String])
              .map(row => (if includeLineNumber then row.set(LineNumberColumn, row.line.map(_.toString).getOrElse("")) else row).toMap)
    )

