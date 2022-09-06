package io.github.artkostm

import org.apache.spark.sql.types.{DoubleType, LongType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{broadcast, col, collect_set, split, struct}
import org.elasticsearch.spark.sql._

object ApplicationSideJoin {
  def main(args: Array[String]): Unit = {
    val _ = SparkSession.builder()
      .master("local[*]")
      .config("spark.es.nodes.wan.only", "true")
      .config("spark.es.index.auto.create", "true")
      .appName("ApplicationSideJoin")
      .getOrCreate()

    args.toList match {
      case moviesPath :: tagsPath :: ratingsPath :: esHost :: esPort :: index :: _ =>
        for {
          movies  <- reader.readMovieLensData(moviesPath, MoviesSchema).map(_.withColumn("genres", split(col("genres"), "\\|")))
          tags    <- reader.readMovieLensData(tagsPath, TagsSchema)
          ratings <- reader.readMovieLensData(ratingsPath, RatingsSchema)
        } yield
          publisher.publishDenormalized(movies, tags, ratings, index, Map("es.nodes" -> esHost, "es.port" -> esPort))
      case p => throw new RuntimeException(s"Please check your cli params: $p. Should be <moviesPath> <tagsPath> <ratingsPath> <esHost> <esPort> <indexName>")
    }
  }

  val MoviesSchema: StructType = StructType(Array(
    StructField("movieId", LongType),
    StructField("title", StringType),
    StructField("genres", StringType),
  ))

  val TagsSchema: StructType = StructType(Array(
    StructField("userId", LongType),
    StructField("movieId", LongType),
    StructField("tag", StringType),
    StructField("timestamp", LongType),
  ))

  val RatingsSchema: StructType = StructType(Array(
    StructField("userId", LongType),
    StructField("movieId", LongType),
    StructField("rating", DoubleType),
    StructField("timestamp", LongType),
  ))
}

object reader {
  def readMovieLensData(csvFilePath: String, schema: StructType): Option[DataFrame] =
    SparkSession.getActiveSession.map { implicit spark =>
      spark.read.schema(schema)
        .options(Map("header" -> "true"))
        .csv(csvFilePath)
    }
}

object publisher {

  def publishDenormalized(movies: DataFrame, tags: DataFrame, ratings: DataFrame, index: String, esConfig: Map[String, String]): Unit = {
    val tagsPrepared = tags.withColumn("tag", struct(col("tag"), col("timestamp")))
      .groupBy("movieId", "userId")
      .agg(collect_set("tag").as("tags"))

    ratings
      .withColumnRenamed("timestamp", "rating_timestamp")
      .join(broadcast(movies), Seq("movieId"))
      .join(tagsPrepared, Seq("movieId", "userId"))
      .saveToEs(index, esConfig)
  }
}