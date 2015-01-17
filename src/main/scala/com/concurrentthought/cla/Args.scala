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
 * to construct initial instances correctly. Subsequent calls to `Args.parse` return
 * new, updated instances.
 */
case class Args protected (
  programInvocation: String,
  description:       String,
  opts:              Seq[Opt[_]],
  defaults:          Map[String,Any],
  values:            Map[String,Any],
  allValues:         Map[String,Seq[Any]],
  remaining:         Seq[String],
  failures:          Seq[(String,Any)]) {

  def help: String = Help(this)

  val requiredOptions = opts.filter(o => o.required)

  // We want the unknownOptionMatch to be just before the "remaining" option.
  protected val remainingOpt = opts.find(_.flags.size == 0).get
  protected val remainingOptName = remainingOpt.name
  protected val opts2: Seq[Opt[_]] = opts.filter(_.name != remainingOptName)
  
  lazy val parserChain: Opt.Parser[_] =
    opts2.map(_.parser) reduceLeft (_ orElse _) orElse unknownOptionMatch orElse remainingOpt.parser

  /**
   * Parse the user-specified arguments, using `parserChain`. Note that if
   * an unrecognized flag is found, i.e., a string that starts with one or two
   * '-', it is an error. Otherwise, all unrecognized options are added to the
   * resulting `values` in a `Seq[String]` with the key, "remaining".
   */
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
        // Otherwise, assume that attempting to parse the value failed, but
        // perhaps it's the next option?
        case NonFatal(nf) => (seq.head, nf) +: p(seq.tail)
      }
    }

    val (failures, successes) = p(args) partition {
      case (_, NonFatal(nf)) => true
      case _ => false
    }

    // The "remaining" values aren't included in the values and allValues maps,
    // but handled separately.
    val newAllValues = allValues ++ successes.foldLeft(Map.empty[String,Vector[Any]]){
      case (map, (key, value)) =>
        val newVect = map.get(key) match {
          case None => Vector(value)
          case Some(v) => v :+ value
        }
        map + (key -> newVect)
    } - remainingOptName
    val newValues = values ++ successes.toMap - remainingOptName
    // The remaining defaults are replaced by the new tokens:
    val newRemaining = successes.filter(_._1 == remainingOptName).map(_._2.toString).toVector

    val failures2 = resolveFailures(successes.map(_._1), failures)
    copy(values = newValues, allValues = newAllValues, remaining = newRemaining, failures = failures2)
  }

  /** Ignore all parse errors if help was requested */
  protected def resolveFailures(keys: Seq[String], failures: Seq[(String,Any)]): Seq[(String,Any)] = {
    if (keys.contains(Args.HELP_KEY)) Nil
    else {
      val missing = requiredOptions.filter(o => !keys.contains(o.name))
      if (missing.size == 0) failures
      else {
        val missingOpts = requiredOptions.filter(o => missing.contains(o))
        failures ++ missingOpts.map(o => (o.name, Args.MissingRequiredArgument(o)))
      }
    }
  }

  import scala.reflect.ClassTag


  /**
   * Return the value for the option. This will be either the default specified,
   * if the user did not invoke the option, or the _last_ invocation of the
   * command line option. In other words, if the argument list contains
   * `--foo bar1 --foo bar2`, then `Some("bar2")` is returned.
   * Note: Use `remaining` to get the tokens not associated with a flag.
   * @see getAll
   */
  def get[V : ClassTag](flag: String): Option[V] =
    values.get(flag).map(_.asInstanceOf[V])

  /**
   * Like `get`, but an alternative is specified, if no value for the option
   * exists, so the return value is of type `V`, rather than `Option[V]`.
   * Note: Use `remaining` to get the tokens not associated with a flag.
   */
  def getOrElse[V : ClassTag](flag: String, orElse: V): V =
    values.getOrElse(flag, orElse).asInstanceOf[V]

  /**
   * Return a `Seq` with all values specified for the option. This supports
   * the case where an option can be repeated on the command line.
   * If the user did not specify the option, then default is mapped to a
   * return value as follows:
   * <ol>
   * <li>`None` => `Nil`
   * <li>`Some(x)` => `Seq(x)`
   * </ol>
   * If the user specified one or more invocations, then all of the values
   * are returned in `Seq`. For example, for `--foo bar1 --foo bar2`, then
   * this method returns `Seq("bar1", "bar2")`.
   * Note: Use `remaining` to get the tokens not associated with a flag.
   * @see get
   */
  def getAll[V : ClassTag](flag: String): Seq[V] =
    allValues.getOrElse(flag, Nil).map(_.asInstanceOf[V])

  /**
   * Like `getAll`, but an alternative is specified, if no value for exists.
   * Note: Use `remaining` to get the tokens not associated with a flag.
   */
  def getAllOrElse[V : ClassTag](flag: String, orElse: Seq[V]): Seq[V] =
    allValues.getOrElse(flag, orElse).map(_.asInstanceOf[V])

  /**
   * Print the current values. Before any parsing is done, the values are
   * the defaults. After parsing, they are the defaults overridden by any
   * user-supplied options. If an option is specified multiple times, then
   * the _last_ invocation is shown.
   * Note that the "remaining" arguments are the same in this output and in
   * `printAllValues`.
   * @see printAllValues
   */
  def printValues(out: PrintStream = Console.out): Unit =
    doPrintValues(out, "")(
      key => values.getOrElse(key, ""))

  /**
   * Print all the current values. Before any parsing is done, the values are
   * the defaults. After parsing, they are the defaults overridden by all the
   * user-supplied options. If an option is specified multiple times, then
   * all values are shown.
   * @see printValues
   */
  def printAllValues(out: PrintStream = Console.out): Unit =
    doPrintValues(out, " (all values given)")(
      key => allValues.getOrElse(key, Vector.empty[String]))

  private def doPrintValues[V](out: PrintStream, suffix: String)(get: String => V): Unit = {
    out.println(s"\nCommand line arguments$suffix:")
    val keys = opts.map(_.name)
    val max = keys.maxBy(_.size).size
    val fmt = s"  %${max}s: %s"
    keys.filter(_ != remainingOptName).foreach(key => out.println(fmt.format(key, get(key))))
    out.println(fmt.format(remainingOptName, remaining))
    out.println()
  }


  /**
   * Was the help option invoked?
   * If so, print the help message to the output `PrintStream` and return true.
   * Otherwise, return false. Callers may wish to exit if true is returned.
   */
  def handleHelp(out: PrintStream = Console.out): Boolean =
    get[Boolean](Args.HELP_KEY) match {
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

  protected val unknownOptionRE = "(--?.+)".r

  /** Unknown option that starts with one or two '-' matches! */
  protected val unknownOptionMatch: Opt.Parser[Any] = {
    case unknownOptionRE(flag) +: tail => throw Args.UnrecognizedArgument(flag, tail)
  }

  override def toString = s"""Args:
  |  program invocation: $programInvocation
  |         description: $description
  |                opts: $opts
  |            defaults: $defaults
  |              values: $values
  |           allValues: $allValues
  |           remaining: $remaining
  |            failures: $failures
  |""".stripMargin

}

object Args {

  val HELP_KEY      = "help"
  val REMAINING_KEY = "remaining"

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

    val noFlagOpts = opts.filter(_.flags.size == 0)
    require(noFlagOpts.size <= 1, "At most one option can have no flags, used for all command-line tokens not associated with flags.")

    // Add opts or help at the beginning and "remaining" (no flag) tokens at
    // the end, if necessary. Also, add defaults and values for the extra
    // options, if needed.
    var opts1 = opts.toVector
    var defaults1 = defaults
    var values1 = values
    var remaining1 = Vector.empty[String]
    if (opts1.exists(_.name == HELP_KEY) == false) {
      opts1 = helpFlag +: opts1
      val hf = (HELP_KEY -> false)
      defaults1 = defaults1 + hf
      values1   = values1   + hf
    }
    if (noFlagOpts.size == 0) {
      opts1 = opts1 :+ remainingOpt
    } else {
      // Make sure the remaining values aren't in "defaults1" or "values1", but update "remaining1"
      val noFlagName = noFlagOpts.head.name
      defaults1  -= noFlagName
      values1    -= noFlagName
      remaining1  = noFlagOpts.head.default match {
        case Some(s) => s match {
          case s: Seq[_] => s.map(_.toString).toVector // _ should already be String, but erasure...
          case x => Vector(x.toString)
        }
        case None => Vector.empty[String]
        case x => throw new RuntimeException(s"$x in $noFlagOpts")
      }
    }
    val allValues1 = values1.map{ case (k,v) => (k,Vector(v)) }
    val failures1  = Seq.empty[(String,Any)]
    new Args(programInvocation, description, opts1, defaults1, values1, allValues1, remaining1, failures1)
  }

  // Common options.

  /** Show Help. Normally the program will exit afterwards. */
  val helpFlag = Flag(
    name   = HELP_KEY,
    flags  = Seq("-h", "--h", "--help"),
    help   = "Show this help message.")

  /** Minimize logging and other output. */
  val quietFlag = Flag(
    name  = "quiet",
    flags = Seq("-q", "--quiet"),
    help  = "Suppress some verbose output.")

  /**
   * A special option for "remaining" or "bare" tokens that aren't associated with a flag.
   * Note that it has no flags; only one such option is allowed in an `Args`.
   */
  def makeRemainingOpt(
    name: String = REMAINING_KEY,
    help: String = "All remaining arguments that aren't associated with flags.",
    requiredFlag: Boolean = false) =
      new OptWithValue[String](name = name, flags = Nil, help = help, requiredFlag = requiredFlag)(s => Try(s)) {

        /** Now there are no flags expected as the first token. */
        override val parser: Opt.Parser[String] = {
          case value +: tail => ((name, Success(value)), tail)
        }
      }

  val remainingOpt = makeRemainingOpt()

  /** Socket host and port. */
  def socketOpt(default: Option[(String, Int)] = None, 
      required: Boolean = false) = Opt[(String,Int)](
    name    = "socket",
    flags   = Seq("-s", "--socket"),
    default = default,
    help    = "Socket host:port.",
    requiredFlag = required) { s =>
      val array = s.split(":")
      if (array.length != 2) Failure(Opt.InvalidValueString("--socket", s))
      else {
        val host = array(0)
        Try(array(1).toInt) match {
          case Success(port) => Success(host -> port)
          case Failure(th)   => Failure(Opt.InvalidValueString("--socket", s"$s (not an int?)", Some(th)))
        }
      }
    }

  case class MissingRequiredArgument[T](o: Opt[T])
    extends RuntimeException("") {

    override def toString = 
      s"""Missing required argument: "${o.name}"${flagsString} ${o.help}"""

    protected def flagsString = 
      if (o.flags.size == 0) ""
      else s""" with flags ${o.flags.mkString(" | ")},"""
  }

  case class UnrecognizedArgument(arg: String, rest: Seq[String])
    extends RuntimeException("") {
      override def toString =
        s"Unrecognized argument (or missing value): $arg ${restOfArgs(rest)}"

      private def restOfArgs(rest: Seq[String]) =
        if (rest.size == 0) "(end of arguments)" else s"""(rest of arguments: ${rest.mkString(" ")})"""
    }

  def defaults(opts: Seq[Opt[_]]): Map[String,Any] =
    opts.filter(_.default != None).map(o => (o.name, o.default.get)).toMap
}


