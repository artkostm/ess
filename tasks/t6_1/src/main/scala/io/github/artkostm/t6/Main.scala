package io.github.artkostm.t6

import cats.effect.*
import cats.syntax.all.*
import com.sksamuel.elastic4s.cats.effect.instances.given
import io.github.artkostm.Cli
import io.github.artkostm.data.DataProvider
import io.github.artkostm.data.csv.Csv
import io.github.artkostm.es.{Client, ClientConfig}
import io.github.artkostm.index.Indexer
import io.github.artkostm.search.SearchSampler
import io.github.artkostm.t6.Cli.{CliParams, SearchParams, TransportParams}
import io.github.vigoo.clipp.*
import io.github.vigoo.clipp.catseffect3.*
import io.github.vigoo.clipp.parsers.*
import io.github.vigoo.clipp.syntax.*
import org.legogroup.woof.{*, given}


object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    Clipp.parseOrDisplayUsageInfo(args, Cli.program, ExitCode.Error) { c =>
      for
        _                <- IO.unit // todo try to avoid such an ugly thing
        given Filter      = Filter.atLeastLevel(LogLevel.valueOf(c.level))
        given Printer     = ColorPrinter()
        given Logger[IO] <- DefaultLogger.makeIo(Output.fromConsole)
        exit             <- Client.java[IO](c.client).use { implicit client =>
                              for
                                given DataProvider[IO]  <- DataProvider.csv[IO](c.file, true)
                                given Client[IO]        = client
                                given Indexer[IO]       <- Indexer.make[IO]()
                                given SearchSampler[IO] <- SearchSampler.make[IO]()
                                app                     <- Application.make[IO]
                                _                       <- c match
                                                            case t: TransportParams => app.run(t)
                                                            case s: SearchParams    => app.search(s)
                              yield ExitCode.Success // todo fold the final effect and convert the result into exit code
                            }
      yield exit
    }

object Cli:
  // todo add more params for csv formatting, credentials, schema/mapping definitions
  // todo add params validation
  sealed trait CliParams:
    val client: ClientConfig
    val file: String
    val index: Option[String]
    val level: String
  final case class TransportParams(
      file: String,
      client: ClientConfig,
      index: Option[String],
      id: Option[String],
      schema: Option[String],
      level: String
  ) extends CliParams

  final case class SearchParams(
      file: String,
      client: ClientConfig,
      index: Option[String],
      level: String
  ) extends CliParams

  val dataTransportProgram: Parameter.Spec[CliParams] = for
    filePath <- namedParameter[String]("Csv file", "some/file/path.csv", 'f', "file")
    url      <- namedParameter[String]("ES url", "http://localhost:9200", 'u', "url")
    index    <- optional[String](namedParameter[String]("ES index name", "index-name", 'i', "index"))
    id       <- optional[String](namedParameter[String]("ES document id field", "id", 'd', "d"))
    schema   <- optional[String](
                  namedParameter[String]("Comma-separated list of data types", "String,Int,Float", 's', "schema")
                ) // todo use recursion schemes
    level    <- optional[String](namedParameter[String]("Log level", "Info", 'l', "level"))
  yield TransportParams(
    filePath,
    ClientConfig(url, None, None),
    index,
    id,
    schema,
    level.getOrElse(LogLevel.Info.toString)
  )

  val searchProgram: Parameter.Spec[CliParams] = for
    filePath <- namedParameter[String]("Csv file", "some/file/path.csv", 'f', "file")
    url      <- namedParameter[String]("ES url", "http://localhost:9200", 'u', "url")
    index    <- optional[String](namedParameter[String]("ES index name", "index-name", 'i', "index"))
    level    <- optional[String](namedParameter[String]("Log level", "Info", 'l', "level"))
  yield SearchParams(filePath, ClientConfig(url, None, None), index, level.getOrElse(LogLevel.Info.toString))

  val program: Parameter.Spec[CliParams] = for
    commandName <- command("transport", "search")
    cmd         <- commandName match
                     case "transport" => dataTransportProgram
                     case "search"    => searchProgram
  yield cmd
