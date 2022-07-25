package io.github.artkostm.schema

import cats.implicits.*
import cats.{Applicative, Foldable, Functor, Monad, Traverse}
import io.github.artkostm.schema.DataF.*
import higherkindness.droste.util.DefaultTraverse

sealed trait DataF[A]:
  def traverse[F[_]: Applicative, B](f: A => F[B]): F[DataF[B]] = this match
    case GStruct(fields) =>
      fields
        .foldRight(Map.empty[String, B].pure[F]) { case ((name, v), acc) =>
          (name.pure[F], f(v), acc).mapN { (n, a, m) =>
            m + (n -> a)
          }
        }
        .map(GStruct(_))
    case GString(n)      => (GString[B](n): DataF[B]).pure[F]
    case GInt(n)         => (GInt[B](n): DataF[B]).pure[F]
    case GNull(t)        => (GNull[B](t): DataF[B]).pure[F]

sealed trait GValue[A] extends DataF[A]:
  val value: Any

object DataF:
//  type x = fs2.data.csv.CsvRowDecoder
  final case class GStruct[A](fields: Map[String, A]) extends DataF[A]
  final case class GInt[A](value: Int)                extends GValue[A]
  final case class GString[A](value: Int)             extends GValue[A]
  final case class GNull[A](tpe: String)              extends GValue[A]:
    override val value: Any = null

  given dataFunctor: Functor[DataF] with
    override def map[A, B](fa: DataF[A])(f: A => B): DataF[B] =
      fa match
        case GStruct(fields) => GStruct[B](fields.view.mapValues(f).toMap)
        case GString(v)      => GString[B](v)
        case GInt(v)         => GInt[B](v)
        case GNull(t)        => GNull[B](t)

  given dataTraverse: Traverse[DataF] = new DefaultTraverse[DataF]:
    override def traverse[G[_]: Applicative, A, B](fa: DataF[A])(f: A => G[B]): G[DataF[B]] =
      fa.traverse(f)
