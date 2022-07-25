package io.github.artkostm.schema

import cats.implicits.*
import cats.{Applicative, Functor, Semigroup, Traverse}
import io.github.artkostm.schema.SchemaF.*
import higherkindness.droste.util.DefaultTraverse

sealed trait SchemaF[A]:
  val nullable: Boolean

  def traverse[F[_]: Applicative, B](f: A => F[B]): F[SchemaF[B]] = this match
    case StructF(fields, n) =>
      fields
        .foldRight(Map.empty[String, B].pure[F]) { case ((name, v), acc) =>
          (name.pure[F], f(v), acc).mapN { (n, a, m) =>
            m + (n -> a)
          }
        }
        .map(StructF(_, n))
    case StringF(n)         => (StringF[B](n): SchemaF[B]).pure[F]
    case IntF(n)            => (IntF[B](n): SchemaF[B]).pure[F]

sealed trait ValueF[A, B] extends SchemaF[A]

object SchemaF:
  final case class StructF[A](fields: Map[String, A], nullable: Boolean) extends SchemaF[A]
  final case class StringF[A](nullable: Boolean)                         extends ValueF[A, String]
  final case class IntF[A](nullable: Boolean)                            extends ValueF[A, Int]

  given schemaFunctor: Functor[SchemaF] = new Functor[SchemaF]:
    override def map[A, B](fa: SchemaF[A])(f: A => B): SchemaF[B] = fa match
      case StructF(fields, n) => StructF(fields.view.mapValues(f).toMap, n)
      case StringF(n)         => StringF[B](n)
      case IntF(n)            => IntF[B](n)

  given schemaTraverse: Traverse[SchemaF] = new DefaultTraverse[SchemaF]:
    override def traverse[G[_]: Applicative, A, B](fa: SchemaF[A])(f: A => G[B]): G[SchemaF[B]] =
      fa.traverse(f)

