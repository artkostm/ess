package io.github.artkostm

import java.io.File

package object utils:
  def getFileName(path: String): String = 
    new File(path).getName
