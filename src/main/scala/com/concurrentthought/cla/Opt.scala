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
 * <li>`required` - the user must specify this option on the command line. 
 *     This flag is effectively ignored if a `default` is provided.</li>
 * <li>`parser` - An implementation feature for parsing arguments.</li>
 * </ol>
 */
sealed trait Opt[V] {
  def name:       String
  def flags:      Seq[String]
  def help:       String
  def default:    Option[V]
  def parser:     Opt.Parser[V]

  protected def requiredFlag:   Boolean

  /** Is the required flag true _and_ the default is None? */
  def required: Boolean = requiredFlag && default == None

  require (name.length != 0, "The Opt name can't be empty.")
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
   * Lift `String => V` to `String => Seq[V]`.
   */
  def toSeq[V](to: String => V): String => Seq[V] = s => Vector(to(s))

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

  /**
   * Construct an option that takes a value. For an option that doesn't
   * take a value, see `Flag`.
   */
  def apply[V](
    name:         String,
    flags:        Seq[String],
    default:      Option[V] = None,
    help:         String = "",
    requiredFlag: Boolean = false)(fromString: String => Try[V]) =
      OptWithValue[V](name, flags, default, help, requiredFlag)(fromString)

  // Helper methods to create options.

  /** Create a String option */
  def string(
    name:         String,
    flags:        Seq[String],
    default:      Option[String] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(identity))

  /** Create a Byte option. */
  def byte(
    name:         String,
    flags:        Seq[String],
    default:      Option[Byte] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_.toByte))

  /** Create a Char option. Just takes the first character in the value string. */
  def char(
    name:         String,
    flags:        Seq[String],
    default:      Option[Char] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_(0)))

  /** Create an Int option. */
  def int(
    name:         String,
    flags:        Seq[String],
    default:      Option[Int] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_.toInt))

  /** Create a Long option. */
  def long(
    name:         String,
    flags:        Seq[String],
    default:      Option[Long] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_.toLong))

  /** Create a Float option. */
  def float(
    name:         String,
    flags:        Seq[String],
    default:      Option[Float] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_.toFloat))

  /** Create a Double option. */
  def double(
    name:         String,
    flags:        Seq[String],
    default:      Option[Double] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      apply(name, flags, default, help, requiredFlag)(toTry(_.toDouble))

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
    name:         String,
    flags:        Seq[String],
    default:      Option[Seq[V]] = None,
    help:         String = "",
    requiredFlag: Boolean = false)(fromString: String => Try[V]) = {
      require (delimsRE.trim.length > 0, "The delimiters RE string can't be empty.")
      apply(name, flags, default, help, requiredFlag) {
        s => seqSupport(name, s, delimsRE, fromString)
      }
    }

  /**
   * A helper method when the substrings are returned without further processing required.
   */
  def seqString(delimsRE:  String)(
    name:         String,
    flags:        Seq[String],
    default:      Option[Seq[String]] = None,
    help:         String = "",
    requiredFlag: Boolean = false) =
      seq[String](delimsRE)(name, flags, default, help, requiredFlag)(toTry(_.toString))

  /**
   * A helper method for path-like structures, where the default delimiter
   * for the platform is used, e.g., ':' for *nix systems and ';' for Windows.
   */
  def path(
    name:         String,
    flags:        Seq[String],
    default:      Option[Seq[String]] = None,
    help:         String = "List of file system paths",
    requiredFlag: Boolean = false) =
      seqString(sys.props.getOrElse("path.separator",":"))(
        name, flags, default, help, requiredFlag)

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
 * A command line argument where an explicit value should follow.
 * In addition to the fields in `Opt`, this type adds the following:
 * <ol>
 * <li>`fromString: String => V` - convert the found value from a String to the correct type.</li>
 * </ol>
 */
case class OptWithValue[V] (
  name:         String,
  flags:        Seq[String],
  default:      Option[V] = None,
  help:         String = "",
  requiredFlag: Boolean = false)(fromString: String => Try[V]) extends Opt[V] {

  protected val opteqRE = "^([^=]+)=(.*)$".r

  val parser: Opt.Parser[V] = {
    case opteqRE(flag, value) +: tail if flags.contains(flag) => parserHelper(flag, value, tail)
    case flag +: value +: tail if flags.contains(flag) => parserHelper(flag, value, tail)
  }

  protected def parserHelper(flag: String, value: String, tail: Seq[String]) = fromString(value) match {
    case sv @ Success(v)  => ((name, sv), tail)
    case Failure(ex) => ex match {
      case ivs: Opt.InvalidValueString => ((name, Failure(ivs)), tail)
      case ex => ((name, Failure(Opt.InvalidValueString(flag, value, Some(ex)))), tail)
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
  name:         String,
  flags:        Seq[String],
  help:         String = "",
  requiredFlag: Boolean = false) extends Opt[Boolean] {

    val default = Some(false)

    // In Scala 2.11, we would simply put default.get in the definition of parser,
    // but in 2.10, we trip over a parse ambiguity of some kind...
    private lazy val d = default.get

    val parser: Opt.Parser[Boolean] = {
      case flag +: tail if flags.contains(flag) => ((name, Success(!d)), tail)
    }
}

/** Companion object for `Flag`. */
object Flag {
  /**
   * Like `apply`, but the value is "flipped"; it defaults to `true`, but if the
   * user supplies the flag, the value is `false`.
   */
  def reverseSense(
  name:         String,
  flags:        Seq[String],
  help:         String = "",
  requiredFlag: Boolean = false) = new Flag(name, flags, help, requiredFlag) {
    override val default = Some(true)
  }
}
