package io.github.artkostm

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.StructType
import org.elasticsearch.spark.sql._

object DataUtils {
  def csvToEs(
               spark: SparkSession,
               csvDataPath: String,
               csvOptions: Map[String, String] = Map.empty,
               sourceSchema: Option[StructType] = None,
               esResource: String,
               esConfig: Map[String, String] = Map.empty,
               transform: DataFrame => DataFrame = identity
             ): DataFrame = {
    val sourceDf = 
      sourceSchema.fold(spark.read)(schema => spark.read.schema(schema))
        .options(csvOptions)
        .csv(csvDataPath)
        .transform(transform)

    sourceDf.saveToEs(esResource, esConfig)
    sourceDf
  }
}
