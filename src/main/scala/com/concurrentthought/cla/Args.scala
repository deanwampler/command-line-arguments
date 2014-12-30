package com.concurrentthought.cla
import scala.util.control.NonFatal
import java.io.PrintStream

/**
 * Contains the options defined, the current values, which are the defaults
 * before parsing and afterwards are the defaults overridden by the actual
 * invocation options, and contains any parsing errors that were found, which
 * is empty before parse is called. In order to properly construct the default
 * values, this constructor is protected. Instead, use the companion Args.apply
 * to construct initial instances correctly. Subsequent calls to `parse` return
 * new, updated instances.
 */
case class Args protected (
  opts: Seq[Opt[_]],
  defaults: Map[String,Any] = Map.empty,
  values: Map[String,Any] = Map.empty,
  failures: Seq[(String,Any)] = Nil) {

  def optsWithHelp = Opt.helpFlag +: opts

  private val helpParser: Opt.Parser[Any] = Opt.helpFlag.parser

  lazy val parserChain: Opt.Parser[Any] = (opts foldLeft helpParser) {
      (partialfunc, opt) => partialfunc orElse opt.parser
    } orElse noMatch

  def parse(args: Seq[String]): Args = {
    def p(args2: Seq[String]): Seq[(String, Any)] = args2 match {
      case Nil => Nil
      case seq => try {
        parserChain(seq) match {
          case ((flag, value), tail) => (flag, value) +: p(tail)
        }
      } catch {
          case e @ Args.UnrecognizedArgument(head, tail) => (head, e) +: p(tail)
          // Otherwise, assume that attempting to parse the value failed, so
          // we skip to seq.tail.tail.
          case NonFatal(nf) => (seq.head, nf) +: p(seq.tail.tail)
      }
    }

    val (failures, successes) = p(args) partition {
      case (_, NonFatal(nf)) => true
      case _ => false
    }
    copy(values = Args.defaults(opts) ++ successes.toMap, failures = failures)
  }

  import scala.reflect.ClassTag

  def get[V : ClassTag](flag: String): Option[V] =
    values.get(flag).map(_.asInstanceOf[V])

  def getOrElse[V : ClassTag](flag: String, orElse: V): V =
    values.getOrElse(flag, orElse).asInstanceOf[V]

  /**
   * Print the current values. Before any parsing is done, they values are
   * the defaults. After parsing, they are the defaults overridden by any
   * user-supplied options.
   */
  def printValues(help: Help, out: PrintStream = Console.out): Unit = {
    out.println("Command line arguments:")
    val keys = values.keySet.toSeq.sorted
    val max = keys.maxBy(_.size).size
    val fmt = s"  %${max}s: %s"
    keys.foreach(key => out.println(fmt.format(key, values(key))))
  }

  /**
   * Was the help option invoked?
   * If so, print the help message to the output `PrintStream` and return true.
   * Otherwise, return false. Callers may wish to exit if true is returned.
   */
  def handleHelp(help: Help, out: PrintStream = Console.out): Boolean =
    get[Boolean]("help") match {
      case Some(true) => out.println(help(this)); true
      case _ => false
    }

  /**
   * Were errors found in the argument list?
   * If so, print the error messages, followed by the  help message and return true.
   * Otherwise, return false. Callers may wish to exit if true is returned.
   */
  def handleErrors(help: Help, out: PrintStream = Console.err): Boolean =
    if (failures.size > 0) {
      out.println(help(this))
      true
    }
    else false

  /** No match! */
  protected val noMatch: Opt.Parser[Any] = {
    case head +: tail => throw Args.UnrecognizedArgument(head, tail)
  }
}

object Args {

  def apply(opts: Seq[Opt[_]]): Args = {
    def defs = defaults(opts)
    apply(opts, defs, defs)
  }

  def apply(opts: Seq[Opt[_]], defaults: Map[String,Any]): Args =
    apply(opts, defaults, defaults)

  def apply(opts: Seq[Opt[_]], defaults: Map[String,Any], values: Map[String,Any]): Args = {
    val defaults2 = defaults + ("help" -> defaults.getOrElse("help", false))
    val valuesHelp = values.getOrElse("help", defaults2("help"))
    val values2   = values   + ("help" -> valuesHelp)
    new Args(Opt.helpFlag +: opts, defaults2, values2)
  }

  case class UnrecognizedArgument(arg: String, rest: Seq[String])
    extends RuntimeException("") {
      override def toString =
        s"UnrecognizedArgument (or missing value): $arg ${restOfArgs(rest)}"

      private def restOfArgs(rest: Seq[String]) =
        if (rest.size == 0) "(end of arguments)" else s"""(rest of arguments: ${rest.mkString(" ")})"""
    }

  def defaults(opts: Seq[Opt[_]]): Map[String,Any] =
    opts.filter(_.default != None).map(o => (o.name, o.default.get)).toMap
}


