package com.concurrentthought.cla
import scala.util.{Try, Success, Failure}

/**
 * Abstraction or a command-line option, with or without a corresponding value.
 * It has the following fields:
 * <ol>
 * <li>`name` - serve as a lookup key for retrieving the value.</li>
 * <li>`flags` - the arguments that invoke the option, e.g., `-h` and `--help`.</li>
 * <li>`help` - a message displayed for command-line help.</li>
 * <li>`default` - an optional default value, for when the user doesn't specify the option.</li>
 * <li>`parser` - An implementation feature for parsing arguments.</li>
 * </ol>
 */
sealed trait Opt[V] {
  val name:    String
  val flags:   Seq[String]
  val help:    String
  val default: Option[V]
  val parser:  Opt.Parser[V]

  require (name.length != 0, "The Opt name can't be empty.")
  require (flags.length != 0, "The Opt must have one or more flags.")
}

object Opt {
  /**
   * Each option attempts to parse one or more tokens in the argument list.
   * If successful, it returns the option's name and extracted value as a tuple,
   * along with the rest of the arguments.
   */
  type Parser[V] = PartialFunction[Seq[String], ((String, Try[V]), Seq[String])]

  /**
   * Lift `String => V` to `String => Try[V]`.
   */
  def toTry[V](to: String => V): String => Try[V] = s => Try(to(s))

  /**
   * Exception raised when an invalid value string is given. Not all errors
   * are detected and reported this way. For example, calls to `s.toInt` for
   * an invalid string will result in `NumberFormatException`.
   */
  case class InvalidValueString(
    flag: String, valueMessage: String, cause: Option[Throwable] = None)
    extends RuntimeException(s"$valueMessage for option $flag",
      if (cause == None) null else cause.get) {

      /** Override toString to show a nicer name for the exception than the FQN. */
      override def toString = {
        val causeStr = if (cause == None) "" else s" (cause: ${cause.get})"
        "Invalid value string: " + getMessage + causeStr
      }
    }

  def apply[V](
    name:    String,
    flags:   Seq[String],
    default: Option[V] = None,
    help:    String = "")(fromString: String => Try[V]) =
      OptWithValue(name, flags, default, help)(fromString)

  // Common options.

  /** Show Help. Normally the program will exit afterwards. */
  val helpFlag = Flag(
    name   = "help",
    flags  = Seq("-h", "--h", "--help"),
    help   = "Show this help message.")

  /** Minimize logging and other output. */
  val quietFlag = Flag(
    name  = "quiet",
    flags = Seq("-q", "--quiet"),
    help  = "Minimize output messages.")

  /** Socket host and port. */
  val socketFlag = Opt[(String,Int)](
    name  = "socket",
    flags = Seq("-s", "--socket"),
    help  = "Socket host:port.") { s =>
      val array = s.split(":")
      if (array.length != 2) Failure(InvalidValueString("--socket", s))
      else {
        val host = array(0)
        Try(array(1).toInt) match {
          case Success(port) => Success(host -> port)
          case Failure(th)   => Failure(InvalidValueString("--socket", s"$s (not an int?)", Some(th)))
        }
      }
    }

  // Helper methods to create options.

  /** Create a String option */
  def string(
    name:    String,
    flags:   Seq[String],
    default: Option[String] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(identity))

  /** Create a Char option. Just takes the first character in the value string. */
  def char(
    name:    String,
    flags:   Seq[String],
    default: Option[Char] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_(0)))

  /** Create a Byte option. */
  def byte(
    name:    String,
    flags:   Seq[String],
    default: Option[Byte] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_.toByte))

  /** Create an Int option. */
  def int(
    name:    String,
    flags:   Seq[String],
    default: Option[Int] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_.toInt))

  /** Create a Long option. */
  def long(
    name:    String,
    flags:   Seq[String],
    default: Option[Long] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_.toLong))

  /** Create a Float option. */
  def float(
    name:    String,
    flags:   Seq[String],
    default: Option[Float] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_.toFloat))

  /** Create a Double option. */
  def double(
    name:    String,
    flags:   Seq[String],
    default: Option[Double] = None,
    help:    String = "") = apply(name, flags, default, help)(
      toTry(_.toDouble))

  /**
   * Create an option where the value string represents a sequence with a delimiter.
   * The delimiter string is treated as a regex. For matching on several possible
   * delimiter characters, use "[;-_]", for example. The resulting substrings won't
   * be trimmed of whitespace, in case you want it, but you can also remove any
   * internal whitespace (i.e., not at the beginning or end of the input string),
   * e.g., "\\s*[;-_]\\s*". The delimiter is given as a separate argument list so
   * that the list of common Opt arguments is consistent with the other helper
   * methods.
   */
  def seq[V](delimsRE:  String)(
    name:    String,
    flags:   Seq[String],
    default: Option[Seq[V]] = None,
    help:    String = "")(fromString: String => Try[V]) = {
      require (delimsRE.trim.length > 0, "The delimiters RE string can't be empty.")
      apply(name, flags, default, help) {
        s => seqSupport(name, s, delimsRE, fromString)
      }
    }

  /**
   * A helper method when the substrings are returned without further processing required.
   */
  def seqString(delimsRE:  String)(
    name:    String,
    flags:   Seq[String],
    default: Option[Seq[String]] = None,
    help:    String = "") =
      seq[String](delimsRE)(name, flags, default, help)(toTry(_.toString))

  private def seqSupport[V](name: String, str: String, delimsRE: String,
    fromString: String => Try[V]): Try[Seq[V]] = {
    def f(strs: Seq[String], vect: Vector[V]): Try[Vector[V]] = strs match {
      case head +: tail => fromString(head) match {
        case Success(value) => f(tail, vect :+ value)
        case Failure(ex) => Failure(ex)
      }
      case Nil => Success(vect)
    }
    f(str.split(delimsRE), Vector.empty[V])
  }

}

/**
 * A command line argument where an explicit value should follow. In addition to
 * the fields in `Opt`, this type adds the following:
 * <ol>
 * <li>`fromString: String => V` - convert the found value from a String to the correct type.</li>
 * </ol>
 */
case class OptWithValue[V](
  name:    String,
  flags:   Seq[String],
  default: Option[V] = None,
  help:    String = "")(fromString: String => Try[V]) extends Opt[V] {

  val parser: Opt.Parser[V] = {
    case flag +: value +: tail if flags.contains(flag) => fromString(value) match {
      case sv @ Success(v)  => ((name, sv), tail)
      case Failure(ex) => ex match {
        case ivs: Opt.InvalidValueString => ((name, Failure(ivs)), tail)
        case ex => ((name, Failure(Opt.InvalidValueString(flag, value, Some(ex)))), tail)
      }
    }
  }
}

/**
 * An option that is just a flag, with no value. By default, its presence
 * indicates "true" for the corresponding option and if the flag isn't specified
 * by the user, then "false" is indicated. However, you can construct a `Flag`
 * with the sense "flipped" using `Flag.reverseSense()`.
 */
case class Flag (
  name:    String,
  flags:   Seq[String],
  help:    String = "") extends Opt[Boolean] {

    val default = Some(false)

    val parser: Opt.Parser[Boolean] = {
      case flag +: tail if flags.contains(flag) => ((name, Try(!default.get)), tail)
    }
}

/** Companion object for `Flag`. */
object Flag {
  /**
   * Like `apply`, but the value is "flipped"; it defaults to `true`, but if the
   * user supplies the flag, the value is `false`.
   */
  def reverseSense(
  name:    String,
  flags:   Seq[String],
  help:    String = "") = new Flag(name, flags, help) {
    override val default = Some(true)
  }
}
