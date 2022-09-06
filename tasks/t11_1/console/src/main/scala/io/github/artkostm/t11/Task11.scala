package io.github.artkostm.t11

import cats.effect.{MonadCancelThrow, Resource, Sync}
import cats.syntax.all.*
import io.circe.*
import io.circe.parser.*
import io.github.artkostm.es.Client
import io.github.artkostm.tablefy.{Box, table}
import org.legogroup.woof.{*, given}

import java.nio.charset.StandardCharsets
import scala.io.Source

trait Task11[F[_]]:
  def run(indexName: String): F[Unit]

object Task11:
  def apply[F[_]: Task11]: Task11[F] = summon

  def make[F[_]: Client: Logger: Sync]: F[Task11[F]] =
    Sync[F].pure(
      new Task11[F]:
        override def run(indexName: String): F[Unit] =
          for
            _     <- Logger[F].info(s"Match query example")
            resp1 <- Client[F].matchQueryExample(indexName)
            _     <- Logger[F].info(s"\n${renderTable(resp1)}\n")
            _     <- Logger[F].info(s"Fuzzy query example")
            resp2 <- Client[F].fuzzyQueryExample(indexName)
            _     <- Logger[F].info(s"\n${renderTable(resp2)}\n")
            _     <- Logger[F].info(s"Top Rated Movies example")
            resp3 <- Client[F].topRatedExample(indexName)
            _     <- Logger[F].info(s"\n${renderTable(resp3)}\n")
            _     <- Logger[F].info(s"Nested query example")
            resp4 <- Client[F].nestedQueryExample(indexName)
            _     <- Logger[F].info(s"\n${renderTable(resp4)}\n")
            _     <- Logger[F].info(s"Top 10 tags for a movie example")
            resp5 <- Client[F].top10TagsExample(indexName)
            _     <- Logger[F].info(s"\n${renderTable(resp5)}\n")
          yield ()
    )

  private def renderTable(rows: List[Map[String, String]]): String =
    Box.render(
      table(
        rows match
          case Nil     => List(List("No data :("))
          case x :: xs =>
            List(
              x.keys.toList,
              x.values.toList
            ) ::: xs.map(_.values.toList)
      )
    )

/** 1. GET movie_lens/_search { "query": { "match": { "genres": { "query": "Comedy" } } } }
  * com.sksamuel.elastic4s.ElasticDsl.search("test_index").matchQuery("genres", "Comedy")
  *
  * 2. GET movie_lens/_search { "query": { "fuzzy": { "genres": { "value": "auction", "fuzziness": "AUTO",
  * "prefix_length": 0 } } } } com.sksamuel.elastic4s.ElasticDsl.search("test_index").query(
  * com.sksamuel.elastic4s.ElasticDsl.fuzzyQuery("genres", "auction").fuzziness("AUTO") )
  *
  * 3. GET movie_lens/_search { "size": 0, "query": { "bool": { "must": [ { "match_phrase": { "title": { "query":
  * "Terminator" } } } ] } }, "aggs": { "top_10_tags": { "nested": { "path": "tags" }, "aggs": {
  * "top_10_tags_for_Terminator": { "terms": { "field": "tags.tag", "size": 10 } } } } } }
  * com.sksamuel.elastic4s.ElasticDsl.search("test_index").size(0).query(
  * com.sksamuel.elastic4s.ElasticDsl.boolQuery().must( com.sksamuel.elastic4s.ElasticDsl.matchPhraseQuery("title",
  * "Terminator") ) ).aggs( com.sksamuel.elastic4s.ElasticDsl.nestedAggregation("top_10_tags", "tags") .subAggregations(
  * com.sksamuel.elastic4s.ElasticDsl.termsAgg("top_10_tags_for_Terminator", "tags.tag").size(10) ) )
  *
  * 4. GET movie_lens/_search { "query": { "bool": { "should": [ { "match": { "userId": { "query": "6285" } } }, {
  * "range": { "rating": { "gte": 5 } } } ] } } }
  *
  * 5. GET movie_lens/_search { "query": { "nested": { "path": "tags", "query": { "match": { "tags.tag": { "query":
  * "good" } } } } } }
  */
