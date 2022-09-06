package io.github.artkostm.es

import com.sksamuel.elastic4s.Indexes
import com.sksamuel.elastic4s.requests.searches.term.WildcardQuery as EsWildcardQuery
import com.sksamuel.elastic4s.requests.searches.{GeoPoint as EsGeoPoint, SearchRequest as EsSearchRequest}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery as EsBooleanQuery
import com.sksamuel.elastic4s.requests.searches.queries.geo.{
  GeoBoundingBoxQuery as EsGeoBoundingBoxQuery,
  Corners as EsCorners
}
import com.sksamuel.elastic4s.requests.searches.queries.matches.MatchQuery as EsMatchQuery
import com.sksamuel.elastic4s.requests.searches.queries.{
  RangeQuery as EsRangeQuery,
  RegexQuery as EsRegexQuery,
  FuzzyQuery as EsFuzzyQuery,
  ScriptQuery as EsScriptQuery,
  Query as EsQuery
}
import com.sksamuel.elastic4s.requests.script.Script
import io.circe.*
import cats.syntax.functor.*
import io.circe.syntax.*
import io.circe.generic.semiauto.*

import scala.concurrent.duration.Duration

// todo: check auto derivation for Conversion[X, Y]
package object model:
  case class SearchRequest(
      indexes: Seq[String],
      pref: Option[String] = None,
      routing: Option[String] = None,
      terminateAfter: Option[Int] = None,
      minScore: Option[Double] = None,
      trackScores: Option[Boolean] = None,
      keepAlive: Option[String] = None,
      query: Option[Query] = None,
      postFilter: Option[Query] = None,
      requestCache: Option[Boolean] = None,
      globalSuggestionText: Option[String] = None,
      from: Option[Int] = None,
      size: Option[Int] = None,
      slice: Option[(Int, Int)] = None,
      explain: Option[Boolean] = None,
      version: Option[Boolean] = None,
      seqNoPrimaryTerm: Option[Boolean] = None,
      profile: Option[Boolean] = None,
      source: Option[String] = None,
      trackHits: Option[Boolean] = None,
      allowPartialSearchResults: Option[Boolean] = None,
      batchedReduceSize: Option[Int] = None,
      typedKeys: Option[Boolean] = None
  )
  object SearchRequest:
    given Decoder[SearchRequest] = deriveDecoder[SearchRequest]
    given Conversion[SearchRequest, EsSearchRequest] with
      override def apply(x: SearchRequest): EsSearchRequest =
        EsSearchRequest(
          indexes = Indexes(values = x.indexes),
          pref = x.pref,
          routing = x.routing,
          terminateAfter = x.terminateAfter,
          minScore = x.minScore,
          trackScores = x.trackScores,
          keepAlive = x.keepAlive,
          query = x.query.map(Query.Conversion.apply),
          postFilter = x.postFilter.map(Query.Conversion.apply),
          requestCache = x.requestCache,
          globalSuggestionText = x.globalSuggestionText,
          from = x.from,
          size = x.size,
          slice = x.slice,
          explain = x.explain,
          version = x.version,
          seqNoPrimaryTerm = x.seqNoPrimaryTerm,
          profile = x.profile,
          source = x.source,
          trackHits = x.trackHits,
          allowPartialSearchResults = x.allowPartialSearchResults,
          batchedReduceSize = x.batchedReduceSize,
          typedKeys = x.typedKeys
        )

  sealed trait Query
  object Query:
    given Decoder[Query]                       =
      List[Decoder[Query]](
        Decoder[RangeQuery].widen,
        Decoder[ScriptQuery].widen,
        Decoder[MatchQuery].widen,
        Decoder[FuzzyQuery].widen,
        Decoder[RegexQuery].widen,
        Decoder[WildcardQuery].widen,
        Decoder[GeoBoundingBoxQuery].widen,
        Decoder[BooleanQuery].widen,
      ).reduceLeft(_ or _)
    val Conversion: Conversion[Query, EsQuery] =
      case q: ScriptQuery         => summon[Conversion[ScriptQuery, EsScriptQuery]](q)
      case q: BooleanQuery        => summon[Conversion[BooleanQuery, EsBooleanQuery]](q)
      case q: MatchQuery          => summon[Conversion[MatchQuery, EsMatchQuery]](q)
      case q: GeoBoundingBoxQuery => summon[Conversion[GeoBoundingBoxQuery, EsGeoBoundingBoxQuery]](q)
      case q: FuzzyQuery          => summon[Conversion[FuzzyQuery, EsFuzzyQuery]](q)
      case q: RegexQuery          => summon[Conversion[RegexQuery, EsRegexQuery]](q)
      case q: WildcardQuery       => summon[Conversion[WildcardQuery, EsWildcardQuery]](q)
      case q: RangeQuery          => summon[Conversion[RangeQuery, EsRangeQuery]](q)

  case class BooleanQuery(
      filter: Query,
      adjustPureNegative: Option[Boolean] = None,
      boost: Option[Double] = None,
      minimumShouldMatch: Option[String] = None,
      queryName: Option[String] = None
  ) extends Query
  object BooleanQuery:
    given Decoder[BooleanQuery] = deriveDecoder[BooleanQuery]
    given Conversion[BooleanQuery, EsBooleanQuery] with
      override def apply(x: BooleanQuery): EsBooleanQuery =
        EsBooleanQuery(
          adjustPureNegative = x.adjustPureNegative,
          boost = x.boost,
          minimumShouldMatch = x.minimumShouldMatch,
          queryName = x.queryName,
          filters = Seq(Query.Conversion.apply(x.filter))
        )

  case class RangeQuery(
      field: String,
      boost: Option[Double] = None,
      timeZone: Option[String] = None,
      lte: Option[Long] = None,
      gte: Option[Long] = None,
      gt: Option[Long] = None,
      lt: Option[Long] = None,
      format: Option[String] = None,
      queryName: Option[String] = None
  ) extends Query
  object RangeQuery:
    given Decoder[RangeQuery] = deriveDecoder[RangeQuery]
    given Conversion[RangeQuery, EsRangeQuery] with
      override def apply(x: RangeQuery): EsRangeQuery =
        EsRangeQuery(
          field = x.field,
          boost = x.boost,
          timeZone = x.timeZone,
          lte = x.lte,
          gte = x.gte,
          gt = x.gt,
          lt = x.lt,
          format = x.format,
          queryName = x.queryName
        )

  case class ScriptQuery(script: String, boost: Option[Double] = None, queryName: Option[String] = None) extends Query
  object ScriptQuery:
    given Decoder[ScriptQuery] = deriveDecoder[ScriptQuery]
    given Conversion[ScriptQuery, EsScriptQuery] with
      override def apply(x: ScriptQuery): EsScriptQuery =
        EsScriptQuery(script = Script(x.script), boost = x.boost, queryName = x.queryName)

  case class MatchQuery(
      field: String,
      value: String,
      analyzer: Option[String] = None,
      boost: Option[Double] = None,
      cutoffFrequency: Option[Double] = None,
      fuzziness: Option[String] = None,
      fuzzyRewrite: Option[String] = None,
      fuzzyTranspositions: Option[Boolean] = None,
      lenient: Option[Boolean] = None,
      maxExpansions: Option[Int] = None,
      minimumShouldMatch: Option[String] = None,
      prefixLength: Option[Int] = None,
      queryName: Option[String] = None,
      zeroTerms: Option[String] = None
  ) extends Query
  object MatchQuery:
    given Decoder[MatchQuery] = deriveDecoder[MatchQuery]
    given Conversion[MatchQuery, EsMatchQuery] with
      override def apply(x: MatchQuery): EsMatchQuery =
        EsMatchQuery(
          field = x.field,
          value = x.value,
          analyzer = x.analyzer,
          boost = x.boost,
          cutoffFrequency = x.cutoffFrequency,
          fuzziness = x.fuzziness,
          fuzzyRewrite = x.fuzzyRewrite,
          fuzzyTranspositions = x.fuzzyTranspositions,
          lenient = x.lenient,
          maxExpansions = x.maxExpansions,
          minimumShouldMatch = x.minimumShouldMatch,
          prefixLength = x.prefixLength,
          queryName = x.queryName,
          zeroTerms = x.zeroTerms
        )

  case class Corners(top: Double, left: Double, bottom: Double, right: Double)
  case class GeoPoint(lat: Double, long: Double)
  case class GeoBoundingBoxQuery(
      field: String,
      corners: Option[Corners] = None,
      geohash: Option[(String, String)] = None,
      topLeft: Option[GeoPoint] = None,
      bottomRight: Option[GeoPoint] = None,
      queryName: Option[String] = None,
      ignoreUnmapped: Option[Boolean] = None
  ) extends Query
  object GeoBoundingBoxQuery:
    given Decoder[Corners]             = deriveDecoder[Corners]
    given Decoder[GeoPoint]            = deriveDecoder[GeoPoint]
    given Decoder[GeoBoundingBoxQuery] = deriveDecoder[GeoBoundingBoxQuery]
    given Conversion[GeoBoundingBoxQuery, EsGeoBoundingBoxQuery] with
      override def apply(x: GeoBoundingBoxQuery): EsGeoBoundingBoxQuery =
        EsGeoBoundingBoxQuery(
          field = x.field,
          corners = x.corners.map(c => EsCorners(c.top, c.left, c.bottom, c.right)),
          geohash = x.geohash,
          cornersOGC = for
            tl <- x.topLeft
            br <- x.bottomRight
          yield (EsGeoPoint(lat = tl.lat, long = tl.long), EsGeoPoint(lat = br.lat, long = br.long)),
          queryName = x.queryName,
          ignoreUnmapped = x.ignoreUnmapped
        )

  case class FuzzyQuery(
      field: String,
      termValue: String,
      fuzziness: Option[String] = None,
      boost: Option[Double] = None,
      transpositions: Option[Boolean] = None,
      maxExpansions: Option[Int] = None,
      prefixLength: Option[Int] = None,
      queryName: Option[String] = None,
      rewrite: Option[String] = None
  ) extends Query
  object FuzzyQuery:
    given Decoder[FuzzyQuery] = deriveDecoder[FuzzyQuery]
    given Conversion[FuzzyQuery, EsFuzzyQuery] with
      override def apply(x: FuzzyQuery): EsFuzzyQuery =
        EsFuzzyQuery(
          field = x.field,
          termValue = x.termValue,
          fuzziness = x.fuzziness,
          boost = x.boost,
          transpositions = x.transpositions,
          maxExpansions = x.maxExpansions,
          prefixLength = x.prefixLength,
          queryName = x.queryName,
          rewrite = x.rewrite
        )

  case class RegexQuery(
      field: String,
      regex: String,
      boost: Option[Double] = None,
      maxDeterminedStates: Option[Int] = None,
      queryName: Option[String] = None,
      rewrite: Option[String] = None,
      caseInsensitive: Option[Boolean] = None
  ) extends Query
  object RegexQuery:
    given Decoder[RegexQuery] = deriveDecoder[RegexQuery]
    given Conversion[RegexQuery, EsRegexQuery] with
      override def apply(x: RegexQuery): EsRegexQuery =
        EsRegexQuery(
          field = x.field,
          regex = x.regex,
          boost = x.boost,
          maxDeterminedStates = x.maxDeterminedStates,
          queryName = x.queryName,
          rewrite = x.rewrite,
          caseInsensitive = x.caseInsensitive
        )

  case class WildcardQuery(
      field: String,
      query: String,
      boost: Option[Double] = None,
      queryName: Option[String] = None,
      rewrite: Option[String] = None,
      caseInsensitive: Option[Boolean] = None
  ) extends Query
  object WildcardQuery:
    given Decoder[WildcardQuery] = deriveDecoder[WildcardQuery]
    given Conversion[WildcardQuery, EsWildcardQuery] with
      override def apply(x: WildcardQuery): EsWildcardQuery =
        EsWildcardQuery(
          field = x.field,
          query = x.query,
          boost = x.boost,
          queryName = x.queryName,
          rewrite = x.rewrite,
          caseInsensitive = x.caseInsensitive
        )
