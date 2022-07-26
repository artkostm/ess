package io.github.artkostm.tablefy

import cats.Show

enum Content:// derives Show:
  case Blank                  // No content.
  case Text(inner: String)    // A raw string.
  case Row(inner: List[Box]) // A row of sub-boxes.
  case Col(inner: List[Box]) // A column of sub-boxes.
  case SubBox(horizontal: Alignment, vertical: Alignment, inner: Box) // A sub-box with a specified alignment.
