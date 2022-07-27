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
  .aggregate(schema, t6_1, t6_1_spark)

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
//    libraryDependencies += "com.github.pureconfig" %% "pureconfig-core" % "0.17.1",
    libraryDependencies += "org.gnieh" %% "fs2-data-csv" % fs2CsvVersion,
    libraryDependencies += "co.fs2" %% "fs2-io" % fs2IoVersion,
    libraryDependencies += "io.higherkindness" %% "droste-core" % drosteVersion,
  )
  .settings(generateBoilerplate: _*)
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
    libraryDependencies += "org.elasticsearch" %% "elasticsearch-spark-30" % "8.3.2" % "provided",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala-elasticsearch" % "0.40.9" % Test,
    libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.9" % Test,
  )

lazy val schema = (project in file("modules/schema"))
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% "cats-mtl" % catsMtlVersion,
    libraryDependencies += "io.higherkindness" %% "droste-core" % drosteVersion,
  )

val generateBoilerplate = Seq(
  Compile / sourceGenerators += Def.task { Boilerplate.gen((Compile / sourceManaged).value) }
)