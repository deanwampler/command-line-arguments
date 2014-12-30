package com.concurrentthought.cla
import java.io._

class StringOut {

  private val bytes = new ByteArrayOutputStream()
  val out: PrintStream = new PrintStream(bytes)

  override def toString = bytes.toString("ISO-8859-1")
}

