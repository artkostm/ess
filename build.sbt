import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerHelper._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.3"
name := "search"

val catsVersion = "2.8.0"
val catsEffectVersion = "3.3.13"
val catsMtlVersion = "1.3.0"
val clippVersion = "0.6.6"
val woofVersion = "0.4.5"
val esClientVersion = "8.2.1"
val fs2CsvVersion = "1.4.1"
val fs2IoVersion = "3.2.10"
val sparkVersion = "3.3.0"
val drosteVersion = "0.9.0"


lazy val root = (project in file("."))
  .aggregate(schema, t6_1, t6_1_spark, t7_1, tablefy)

lazy val t6_1 = (project in file("tasks/t6_1"))
  .settings(
    name := "t6_1",
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% "cats-mtl" % catsMtlVersion,
    libraryDependencies += "io.github.vigoo" %% "clipp-cats-effect3" % clippVersion,
    libraryDependencies += "org.legogroup" %% "woof-core" % woofVersion,
    libraryDependencies += ("com.sksamuel.elastic4s" %% "elastic4s-core" % esClientVersion)
      .exclude("org.typelevel", "cats-effect-std_2.13")
      .exclude("org.typelevel", "cats-effect_2.13")
      .exclude("org.typelevel", "cats-kernel_2.13")
      .exclude("org.typelevel", "cats-effect-kernel_2.13")
      .exclude("org.typelevel", "cats-core_2.13")
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.sksamuel.elastic4s" %% "elastic4s-effect-cats" % esClientVersion)
      .exclude("org.typelevel", "cats-effect-std_2.13")
      .exclude("org.typelevel", "cats-effect_2.13")
      .exclude("org.typelevel", "cats-kernel_2.13")
      .exclude("org.typelevel", "cats-effect-kernel_2.13")
      .exclude("org.typelevel", "cats-core_2.13")
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += ("com.sksamuel.elastic4s" % "elastic4s-client-esjava" % esClientVersion)
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += "org.gnieh" %% "fs2-data-csv" % fs2CsvVersion,
    libraryDependencies += "co.fs2" %% "fs2-io" % fs2IoVersion,
    libraryDependencies += "io.higherkindness" %% "droste-core" % drosteVersion,
  )
  .settings(scalacOptions ++= Seq(
    "-source:future"
  ))
  .dependsOn(schema)

lazy val t6_1_spark = (project in file("tasks/t6_1_spark"))
  .settings(
    name := "t6_1_spark",
    scalaVersion := "2.13.8",
    Test / fork := true,
    libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion,// % "provided",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion,// % "provided",
    libraryDependencies += "org.elasticsearch" %% "elasticsearch-spark-30" % "8.3.2" % Provided,
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala-elasticsearch" % "0.40.9" % Test,
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.9" % Test,
  )

lazy val t7_1 = (project in file("tasks/t7_1"))
  .settings(
    name := "t7_1",
    libraryDependencies += "org.junit.jupiter" % "junit-jupiter-engine" % "5.8.1" % Test,
    libraryDependencies += "org.elasticsearch" % "elasticsearch" % "8.1.0" % Provided,
    Universal / packageName := "roman2arabic-" + version.value,
    Universal / mappings := {
      val pluginDescriptor = sourceDirectory.value / "main" / "resources" / "plugin-descriptor.properties"
      val universalMappings = (Universal / mappings).value

      universalMappings.filter {
        case (f, _) => ! f.getName.contains("scala") // remove all the scala runtime libs
      }.map { case (f, _) => f -> f.getName } :+ (pluginDescriptor -> pluginDescriptor.file.getName) // and add the plugin descriptor to the root of the archive
    }
  ).enablePlugins(JavaServerAppPackaging)

lazy val schema = (project in file("modules/schema"))
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% "cats-mtl" % catsMtlVersion,
    libraryDependencies += "io.higherkindness" %% "droste-core" % drosteVersion,
  )

lazy val tablefy = (project in file("modules/tablefy"))
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% "cats-mtl" % catsMtlVersion,
    libraryDependencies += "io.higherkindness" %% "droste-core" % drosteVersion,
    //libraryDependencies += ("org.scala-lang.modules" %% "scala-collection-contrib" % "0.2.2").cross(CrossVersion.for3Use2_13)
  )
