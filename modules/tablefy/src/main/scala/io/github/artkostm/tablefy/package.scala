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
   * import io.github.artkostm.tablefy.*
   * import io.github.artkostm.tablefy.Box.*
   * 
   * unsafePrintBox(
   *   table(
   *     List(List.tabulate(10)(i => s"Header$i")) ::: 
   *       List.tabulate(10)(j => List.tabulate(10)(i => s"${i}x$j"))
   *   )
   * )
   * 
   * Result: 
   * 
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |Header0|Header1|Header2|Header3|Header4|Header5|Header6|Header7|Header8|Header9|
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x0    |1x0    |2x0    |3x0    |4x0    |5x0    |6x0    |7x0    |8x0    |9x0    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x1    |1x1    |2x1    |3x1    |4x1    |5x1    |6x1    |7x1    |8x1    |9x1    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x2    |1x2    |2x2    |3x2    |4x2    |5x2    |6x2    |7x2    |8x2    |9x2    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x3    |1x3    |2x3    |3x3    |4x3    |5x3    |6x3    |7x3    |8x3    |9x3    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x4    |1x4    |2x4    |3x4    |4x4    |5x4    |6x4    |7x4    |8x4    |9x4    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x5    |1x5    |2x5    |3x5    |4x5    |5x5    |6x5    |7x5    |8x5    |9x5    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x6    |1x6    |2x6    |3x6    |4x6    |5x6    |6x6    |7x6    |8x6    |9x6    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x7    |1x7    |2x7    |3x7    |4x7    |5x7    |6x7    |7x7    |8x7    |9x7    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x8    |1x8    |2x8    |3x8    |4x8    |5x8    |6x8    |7x8    |8x8    |9x8    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * |0x9    |1x9    |2x9    |3x9    |4x9    |5x9    |6x9    |7x9    |8x9    |9x9    |
   * +-------+-------+-------+-------+-------+-------+-------+-------+-------+-------+
   * 
   * @return
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
