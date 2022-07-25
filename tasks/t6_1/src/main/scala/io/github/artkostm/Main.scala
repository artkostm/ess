package io.github.artkostm

import org.legogroup.woof.{*, given}
import io.github.vigoo.clipp.*
import io.github.vigoo.clipp.syntax.*
import io.github.vigoo.clipp.parsers.*
import io.github.vigoo.clipp.catseffect3.*
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*
import com.sksamuel.elastic4s.cats.effect.instances.given
import io.github.artkostm.Cli.CliParams
import io.github.artkostm.csv.Csv
import io.github.artkostm.es.ClientConfig
import io.github.artkostm.utils.Config
import io.github.artkostm.es.Client
import io.github.artkostm.index.Indexer

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    Clipp.parseOrDisplayUsageInfo(args, Cli.program, ExitCode.Error) { c =>
      for
        _                <- IO.unit // todo try to avoid such an ugly thing
        given Filter      = Filter.atLeastLevel(LogLevel.valueOf(c.level))
        given Printer     = ColorPrinter()
        given Logger[IO] <- DefaultLogger.makeIo(Output.fromConsole)
        exit             <- Client.java[IO](c.client).use {
                              client =>
                                for
                                  given Csv[IO]     <- Csv.make[IO]
                                  given Client[IO]   = client
                                  given Indexer[IO] <- Indexer.make[IO]()
                                  app               <- Application.make[IO]
                                  _                 <- app.run(c)
                                yield ExitCode.Success //todo fold the final effect and convert the result into exit code
                            }
      yield exit
    }

object Cli:
  // todo add more params for csv formatting, credentials,, schema/mapping definitions
  // todo add params validation
  final case class CliParams(
      file: String,
      client: ClientConfig,
      index: Option[String],
      id: Option[String],
      schema: Option[String],
      level: String
  )

  val program: Parameter.Spec[CliParams] = for
    filePath <- namedParameter[String]("Csv file", "some/file/path.csv", 'f', "file")
    url      <- namedParameter[String]("ES url", "http://localhost:9200", 'u', "url")
    index    <- optional[String](namedParameter[String]("ES index name", "index-name", 'i', "index"))
    id       <- optional[String](namedParameter[String]("ES document id field", "id", 'd', "d"))
    schema   <- optional[String](
                  namedParameter[String]("Comma-separated list of data types", "String,Int,Float", 's', "schema")
                ) // todo use recursion schemes
    level    <- optional[String](namedParameter[String]("Log level", "Info", 'l', "level"))
  yield CliParams(filePath, ClientConfig(url, None, None), index, id, schema, level.getOrElse(LogLevel.Info.toString))
