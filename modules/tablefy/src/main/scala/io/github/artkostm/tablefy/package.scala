package io.github.artkostm

import cats.syntax.monoid.*

package object tablefy:
  import Alignment.*
  import Box.*

  // todo: add table format
  def fmtColumn: List[String] => Box =
    items =>
      val width = items.map(_.length).max
      val hsep = text("-" * width)
      hsep || vcat(left, items.map(i => text(pad(width, i))).intersperse(hsep)) || hsep

  /**
   * The main method to build the table
   *
   * {{{
   * scala>
   * import io.github.artkostm.tablefy.*
   * import io.github.artkostm.tablefy.Box.*
   *
   * unsafePrintBox(
   *   table(
   *     List(List.tabulate(5)(i => s"Header$i")) :::
   *       List.tabulate(5)(j => List.tabulate(5)(i => s"${i}x$j"))
   *   )
   * )
   *
   * Output:
   * +-------+-------+-------+-------+-------+
   * |Header0|Header1|Header2|Header3|Header4|
   * +-------+-------+-------+-------+-------+
   * |0x0    |1x0    |2x0    |3x0    |4x0    |
   * +-------+-------+-------+-------+-------+
   * |0x1    |1x1    |2x1    |3x1    |4x1    |
   * +-------+-------+-------+-------+-------+
   * |0x2    |1x2    |2x2    |3x2    |4x2    |
   * +-------+-------+-------+-------+-------+
   * |0x3    |1x3    |2x3    |3x3    |4x3    |
   * +-------+-------+-------+-------+-------+
   * |0x4    |1x4    |2x4    |3x4    |4x4    |
   * +-------+-------+-------+-------+-------+
   * }}}
   * @return Box
   */
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
