package com.concurrentthought.cla
import scala.util.{Try, Success, Failure}

/**
 * A command-line option, required or not, with a corresponding value.
 * @param name serve as a lookup key for retrieving the value.
 * @param flags the arguments that invoke the option, e.g., `-h` and `--help`.
 * @param help a message displayed for command-line help.
 * @param default an optional default value, for when the user doesn't specify the option.
 * @param required the user must specify this option on the command line.
 *     This flag is effectively ignored if a `default` is provided.
 * @param parser An implementation feature for parsing arguments.
 */
trait Opt[V] {
  val name:     String
  val flags:    Seq[String]
  val default:  Option[V]
  val help:     String
  val required: Boolean

  require (name.length != 0, "The Opt name can't be empty.")

  protected val optEQRE = "^([^=]+)=(.*)$".r

  /** Is the required flag true _and_ the default is None? */
  def isRequired: Boolean = required && default == None

  val parser: Opt.Parser[V]

  /** The minimal step to parse a string into an object, wrapping in a Try. */
  protected def stringToObject: String => Try[V]

  /** Helper for concrete implementations of the parser method. */
  protected def parserHelper(flag: String, value: String, tail: Seq[String]) = stringToObject(value) match {
    case Success(v)  => ((name, Success(v)), tail)
    case Failure(ex) => ex match {
      case ivs: Opt.InvalidValueString => ((name, Failure(ivs)), tail)
      case ex: Any => ((name, Failure(Opt.InvalidValueString(flag, value, Some(ex)))), tail)
    }
  }
}


object Opt {

  val REMAINING_KEY = "remaining"

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
   * Lift `String => V` to `String => Seq[V]`.
   */
  def toSeq[V](to: String => V): String => Seq[V] = s => Vector(to(s))

  /**
   * Exception raised when an invalid value string is given. Not all errors
   * are detected and reported this way. For example, calls to `s.toInt` for
   * an invalid string will result in `NumberFormatException`.
   */
  final case class InvalidValueString(
      flag: String,
      valueMessage: String,
      cause: Option[Throwable] = None)
    extends RuntimeException(s"$valueMessage for option $flag", cause.getOrElse(null)) {

    /** Override toString to show a nicer name for the exception than the FQN. */
    override def toString: String = {
      val causeStr = cause.fold("")(c => s" (cause $c)")
      "Invalid value string: " + getMessage + causeStr
    }
  }

  /**
   * Implementation of an an Opt that has a flag and value.
   * @param fromString convert the found value from a String to the correct type.
   */
  protected[cla] final case class OptImpl[V] (
    name:     String,
    flags:    Seq[String],
    default:  Option[V] = None,
    help:     String = "",
    required: Boolean = false)(
    fromString: String => Try[V]) extends Opt[V] {

    protected def stringToObject: String => Try[V] = fromString

    val parser: Opt.Parser[V] = {
      case optEQRE(flag, value) +: tail if flags.contains(flag) => parserHelper(flag, value, tail)
      case flag +: value +: tail if flags.contains(flag) => parserHelper(flag, value, tail)
    }
  }

  /**
   * An implementation class for flags (boolean) options.
   */
  protected[cla] final case class Flag(
    name:         String,
    flags:        Seq[String],
    defaultValue: Boolean = false,
    help:         String = "",
    required:     Boolean = false) extends Opt[Boolean] {

    val default = Some(defaultValue)

    protected def stringToObject: String => Try[Boolean] = s => Try(s.toBoolean)

    /** For flags, the parser doesn't consume the next token (if any). */
    val parser: Opt.Parser[Boolean] = {
      case flag +: tail if flags.contains(flag) => parserHelper(flag, (!defaultValue).toString, tail)
    }
  }

  /**
   * Implementation of an a special kind of Opt that represents "bare" command-line
   * tokens without flags.
   * @param fromStringtring convert the found value from a String to the correct type.
   */
  protected[cla] final case class BareTokenOpt (
    name:     String,
    help:     String,
    required: Boolean = false) extends Opt[String] {

    val flags: Seq[String] = Nil
    val default: Option[String] = None

    protected def stringToObject: String => Try[String] = s => Try(s)

    /** Now there are no flags expected as the first token. */
    val parser: Opt.Parser[String] = {
      case value +: tail => ((name, Success(value)), tail)
    }
  }

  // Helper methods to create options.

  /**
   * Create a "flag" (Boolean) option. Unlike all the other kinds of
   * options, <em>it does not consume an argument that follows it.</em>
   * Instead the inferred default value corresponding to the flag is false.
   * If a user specifies the flag on the command line, the corresponding value
   * is true.
   * @see Opt.notflag
   */
  def flag(
    name:     String,
    flags:    Seq[String],
    help:     String = "",
    required: Boolean = false): Flag =
      new Flag(name, flags, false, help, required)

  /**
   * Like `flag`, but the default value is true, not false.
   * @see Opt.flag
   */
  def notflag(
    name:     String,
    flags:    Seq[String],
    help:     String = "",
    required: Boolean = false): Flag =
      new Flag(name, flags, true, help, required)

  /** Create a String option */
  def string(
    name:     String,
    flags:    Seq[String],
    default:  Option[String] = None,
    help:     String = "",
    required: Boolean = false): Opt[String] =
      new OptImpl(name, flags, default, help, required)(toTry(identity))

  /** Create a Byte option. */
  def byte(
    name:     String,
    flags:    Seq[String],
    default:  Option[Byte] = None,
    help:     String = "",
    required: Boolean = false): Opt[Byte] =
      new OptImpl(name, flags, default, help, required)(toTry(_.toByte))

  /** Create a Char option. Just takes the first character in the value string. */
  def char(
    name:     String,
    flags:    Seq[String],
    default:  Option[Char] = None,
    help:     String = "",
    required: Boolean = false): Opt[Char] =
      new OptImpl(name, flags, default, help, required)(toTry(_(0)))

  /** Create an Int option. */
  def int(
    name:     String,
    flags:    Seq[String],
    default:  Option[Int] = None,
    help:     String = "",
    required: Boolean = false): Opt[Int] =
      new OptImpl(name, flags, default, help, required)(toTry(_.toInt))

  /** Create a Long option. */
  def long(
    name:     String,
    flags:    Seq[String],
    default:  Option[Long] = None,
    help:     String = "",
    required: Boolean = false): Opt[Long] =
      new OptImpl(name, flags, default, help, required)(toTry(_.toLong))

  /** Create a Float option. */
  def float(
    name:     String,
    flags:    Seq[String],
    default:  Option[Float] = None,
    help:     String = "",
    required: Boolean = false): Opt[Float] =
      new OptImpl(name, flags, default, help, required)(toTry(_.toFloat))

  /** Create a Double option. */
  def double(
    name:     String,
    flags:    Seq[String],
    default:  Option[Double] = None,
    help:     String = "",
    required: Boolean = false): Opt[Double] =
      new OptImpl(name, flags, default, help, required)(toTry(_.toDouble))

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
  def seq[V](delimsRE: String)(
    name:     String,
    flags:    Seq[String],
    default:  Option[Seq[V]] = None,
    help:     String = "",
    required: Boolean = false)(
    fromString: String => Try[V]): Opt[Seq[V]] = {
      require (delimsRE.trim.length > 0, "The delimiters RE string can't be empty.")
      new OptImpl(name, flags, default, help, required)(s => seqSupport(s, delimsRE, fromString))
    }

  /**
   * A helper method when the substrings are returned without further processing required.
   */
  def seqString(delimsRE: String)(
    name:     String,
    flags:    Seq[String],
    default:  Option[Seq[String]] = None,
    help:     String = "",
    required: Boolean = false): Opt[Seq[String]] =
      seq[String](delimsRE)(name, flags, default, help, required)(toTry(_.toString))

  /**
   * A helper method for path-like structures, where the default delimiter
   * for the platform is used, e.g., ':' for *nix systems and ';' for Windows.
   */
  def path(
    name:     String,
    flags:    Seq[String],
    default:  Option[Seq[String]] = None,
    help:     String = "List of file system paths",
    required: Boolean = false): Opt[Seq[String]] =
      seqString(pathSeparator)(
        name, flags, default, help, required)

  def pathSeparator: String = sys.props.getOrElse("path.separator",":")

  private def seqSupport[V](
    str:        String,
    delimsRE:   String,
    fromString: String => Try[V]): Try[Seq[V]] = {
    @annotation.tailrec
    def f(strs: Seq[String], vect: Vector[V]): Try[Vector[V]] = strs match {
      case head +: tail => fromString(head) match {
        case Success(value) => f(tail, vect :+ value)
        case Failure(ex) => Failure(ex)
      }
      case Nil => Success(vect)
    }
    f(str.split(delimsRE).toIndexedSeq, Vector.empty[V])
  }

  /**
   * A helper method for a socket host and port.
   */
  def socket(
    name:     String = "socket",
    flags:    Seq[String] = Seq("-s", "--socket"),
    default:  Option[(String, Int)] = None,
    help:     String = "Socket host:port.",
    required: Boolean = false): Opt[(String,Int)] =
      new OptImpl(name, flags, default, help, required)(socketFromString)

  private def socketFromString(s: String): Try[(String,Int)] = {
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

  /**
   * A helper method for a special kind of "option", one for handling bare token
   * arguments that don't have flags.
   */
  def bareTokens(
    name: String = REMAINING_KEY,
    help: String = "All remaining arguments that aren't associated with flags.",
    required: Boolean = false): Opt[String] =
      new BareTokenOpt(name, help, required)
}

