package io.github.artkostm.csv

import cats.data.NonEmptyList
import io.github.artkostm.schema.{FSchema, SchemaF}
import fs2.data.csv.*
import io.github.artkostm.schema.SchemaF.*
import higherkindness.droste.data.Fix
import higherkindness.droste.syntax.all.toFixSyntaxOps

case class GenericHeader(schema: Option[FSchema] = None) extends ParseableHeader[(String, FSchema)]:
  def apply(names: NonEmptyList[String]): HeaderResult[(String, FSchema)] =
    schema.fold(Right(names.map(s => s -> StringF(true).fix[SchemaF]))) {
      case Fix(StructF(fields, _)) =>
        NonEmptyList
          .fromList(fields.zip(names.toList).map { case ((_, s), column) => column -> s }.toList)
          .toRight(HeaderError("The row schema should have at least one column"))
      case _                       => Left(HeaderError("The row schema should be of StructF type"))
    }
