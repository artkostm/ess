package io.github.artkostm.t11

import cats.effect.*
import cats.syntax.all.*
import com.sksamuel.elastic4s.cats.effect.instances.given
import io.github.artkostm.data.DataProvider
import io.github.artkostm.data.csv.Csv
import io.github.artkostm.es.{Client, ClientConfig}
import io.github.artkostm.index.Indexer
import io.github.artkostm.search.SearchSampler
import io.github.artkostm.t11.Cli.CliParams
import io.github.artkostm.t11.{Application, Cli}
import io.github.vigoo.clipp.*
import io.github.vigoo.clipp.catseffect3.*
import io.github.vigoo.clipp.parsers.*
import io.github.vigoo.clipp.syntax.*
import org.legogroup.woof.{*, given}

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    Clipp.parseOrDisplayUsageInfo(args, Cli.program, ExitCode.Error) { c =>
      for
        _                <- IO.unit
        given Filter      = Filter.atLeastLevel(LogLevel.valueOf(c.level))
        given Printer     = ColorPrinter()
        given Logger[IO] <- DefaultLogger.makeIo(Output.fromConsole)
        exit             <- Client.java[IO](c.client).use { client =>
                              given Client[IO] = client
                              for
                                given Task11[IO] <- Task11.make[IO]
                                app              <- Application.make[IO]
                                _                <- app.run(c)
                              yield ExitCode.Success
                            }
      yield exit
    }

object Cli:
  final case class CliParams(
      client: ClientConfig,
      index: String,
      level: String
  )

  val program: Parameter.Spec[CliParams] = for
    url      <- namedParameter[String]("ES url", "http://localhost:9200", 'u', "url")
    index    <- namedParameter[String]("ES index name", "index-name", 'i', "index")
    level    <- optional[String](namedParameter[String]("Log level", "Info", 'l', "level"))
  yield CliParams(
    ClientConfig(url, None, None),
    index,
    level.getOrElse(LogLevel.Info.toString)
  )
