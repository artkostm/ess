package io.github.artkostm

import cats.syntax.monoid.*

package object tablefy:
  import Alignment.*
  import Box.*

  def fmtColumn: List[String] => Box =
    items =>
      val width = items.map(_.length).max
      val hsep = text("-" * width)
      hsep || vcat(left, items.map(i => text(pad(width, i))).intersperse(hsep)) || hsep

  // todo: add table format
  def table: List[List[String]] => Box =
    rows =>
      val columns = rows.transpose
      val nrows = rows.size
      val vsep = vcat(left, ("+" + ("|+" * nrows)).map(char).toList)
      vsep |+| hcat(top, columns.map(fmtColumn).intersperse(vsep)) |+| vsep

  extension[A] (l: List[A])
    def intersperse[B >: A](sep: B): List[B] =
      val it = l.iterator
      val res: Iterator[B] = new Iterator[B]:
        var intersperseNext = false
        def hasNext: Boolean = intersperseNext || it.hasNext
        def next(): B =
          val elem = if intersperseNext then sep else it.next()
          intersperseNext = !intersperseNext && it.hasNext
          elem

      res.toList

  protected inline def chunksOf[A](n: Int, l: List[A]): List[List[A]] = l.grouped(n).toList

  protected def pad(width: Int, x: String): String = x.padTo(width - x.length, ' ')
