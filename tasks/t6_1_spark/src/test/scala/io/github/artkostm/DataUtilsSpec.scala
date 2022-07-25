package io.github.artkostm

import com.dimafeng.testcontainers.ElasticsearchContainer
import com.dimafeng.testcontainers.munit.TestContainerForAll
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, split, struct}
import org.elasticsearch.spark.sql._
import org.testcontainers.utility.DockerImageName

import java.util.UUID

class DataUtilsSpec extends munit.FunSuite with TestContainerForAll {
  override val containerDef = ElasticsearchContainer.Def(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.9.2"))

  val sparkSession: FunFixture[SparkSession] = FunFixture[SparkSession](
    setup = { _ =>
      SparkSession
        .builder()
        .master("local")
        .config("spark.es.nodes.wan.only", "true")
        .config("spark.es.index.auto.create", "true")
        .getOrCreate()
    },
    teardown = { _.close() }
  )

  sparkSession.test("check ES write/read operations using Spark") { spark =>
    withContainers { esContainer =>
      val Array(host, port) = esContainer.httpHostAddress.split(":")

      val resourceName = s"test_${UUID.randomUUID()}"

      val expectedDf = DataUtils.csvToEs(
        spark,
        csvDataPath = "tasks/t6_1_spark/src/test/resources/test_employee.csv",
        csvOptions = Map("header" -> "true", "nullValue" -> "null"),
        esResource = "employee",
        esConfig = Map("es.mapping.id" -> "upn", "es.nodes" -> host, "es.port" -> port),
        transform = _.withColumn("managers", split(col("managers"), "\\|"))
      )

      val actualDf = spark.esDF(resourceName, Map("es.nodes" -> host, "es.port" -> port))
      compare(expectedDf, actualDf, Seq("upn"))
    }
  }

  private def compare(expected: DataFrame, actual: DataFrame, key: Seq[String]): Unit = {
    assertEquals(actual.columns, expected.columns)
    assertEquals(actual.count(), expected.count())
    val columns = actual.columns
    val diff = expected.as("e").join(actual.as("a"), key, "full_outer")
      .withColumn("test", struct(columns.map(c => col(s"e.$c")): _*) === struct(columns.map(c => col(s"a.$c")): _*))
      .groupBy("test")
      .count()
      .collect()
      .length
    assertEquals(diff, 1, "data frames are not equal \nExpected: " + expected.show(10, false) + "\nActual: " + actual.show(10, false))
  }
}
