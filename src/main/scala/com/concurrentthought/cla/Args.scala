package com.concurrentthought.cla
import scala.util.control.NonFatal
import scala.util.{Try, Success, Failure}
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
  programInvocation: String,
  description: String,
  opts: Seq[Opt[_]],
  defaults: Map[String,Any],
  values: Map[String,Any],
  failures: Seq[(String,Any)]) {

  def help: String = Help(this)

  lazy val parserChain: Opt.Parser[Any] = 
    opts map (_.parser) reduceLeft (_ orElse _) orElse noMatch

  def parse(args: Seq[String]): Args = {
    def p(args2: Seq[String]): Seq[(String, Any)] = args2 match {
      case Nil => Nil
      case seq => try {
        parserChain(seq) match {
          case ((flag, Success(value)), tail) => (flag, value) +: p(tail)
          case ((flag, Failure(failure)), tail) => (flag, failure) +: p(tail)
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
  def printValues(out: PrintStream = Console.out): Unit = {
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
  def handleHelp(out: PrintStream = Console.out): Boolean =
    get[Boolean]("help") match {
      case Some(true) => out.println(help); true
      case _ => false
    }

  /**
   * Were errors found in the argument list?
   * If so, print the error messages, followed by the  help message and return true.
   * Otherwise, return false. Callers may wish to exit if true is returned.
   */
  def handleErrors(out: PrintStream = Console.err): Boolean =
    if (failures.size > 0) {
      out.println(help)
      true
    }
    else false

  /** No match! */
  protected val noMatch: Opt.Parser[Any] = {
    case head +: tail => throw Args.UnrecognizedArgument(head, tail)
  }
}

object Args {

  val defaultProgramInvocation: String = "java -cp ..."
  val defaultDescription: String = ""

  def empty: Args = {
      apply(Args.defaultProgramInvocation, Args.defaultDescription, Nil)
    }

  def apply(opts: Seq[Opt[_]]): Args = {
      apply(Args.defaultProgramInvocation, Args.defaultDescription, opts)
    }

  def apply(
    programInvocation: String,
    description: String,
    opts: Seq[Opt[_]]): Args = {
      def defs = defaults(opts)
      apply(programInvocation, description, opts, defs, defs)
    }

  def apply(
    programInvocation: String,
    description: String,
    opts: Seq[Opt[_]], 
    defaults: Map[String,Any]): Args =
      apply(programInvocation, description, opts, defaults, defaults)

  def apply(
    programInvocation: String,
    description: String,
    opts: Seq[Opt[_]], 
    defaults: Map[String,Any], 
    values: Map[String,Any]): Args = {
      val opts2      = if (opts.exists(_.name == "help")) opts else (Opt.helpFlag +: opts)
      val defaults2  = defaults + ("help" -> defaults.getOrElse("help", false))
      val valuesHelp = values.getOrElse("help", defaults2("help"))
      val values2    = values   + ("help" -> valuesHelp)
      val failures   = Seq.empty[(String,Any)]

      new Args(programInvocation, description, opts2, defaults2, values2, failures)
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


