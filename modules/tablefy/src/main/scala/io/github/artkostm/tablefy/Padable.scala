package io.github.artkostm.tablefy

sealed trait Padable[A]:
  type Filler
  def reverse(a: A): A
  def splitAt(a: A, n: Int): (A, A)
  def length(a: A): Int
  def takeP(filler: Filler, n: Int, value: A): A

protected[tablefy] object Padable:
  inline def apply[A: Padable]: Padable[A] = summon

  given Padable[String] with
    override type Filler = Char
    override def reverse(a: String): String                         = a.reverse
    override def splitAt(a: String, n: Int): (String, String)       = a.splitAt(n)
    override def length(a: String): Int                             = a.length
    override def takeP(filler: Char, n: Int, value: String): String =
      if n <= 0 then ""
      else if n <= value.length then value.take(n)
      else value.padTo(n, filler)

  given [A]: Padable[List[A]] with
    override type Filler = A
    override def reverse(a: List[A]): List[A]                      = a.reverse
    override def splitAt(a: List[A], n: Int): (List[A], List[A])   = a.splitAt(n)
    override def length(a: List[A]): Int                           = a.size
    override def takeP(filler: A, n: Int, value: List[A]): List[A] =
      if n <= 0 then List.empty
      else if n <= value.length then value.take(n)
      else value.padTo(n, filler)
