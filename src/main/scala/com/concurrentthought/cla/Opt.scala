package com.concurrentthought.cla

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
  type Parser[V] = PartialFunction[Seq[String], ((String,V), Seq[String])]

  def apply[V](
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[V] = None)(fromString: String => V) =
      OptWithValue(name, flags, help, default)(fromString)

  // Common options

  /** Show Help. Normally the program will exit afterwards. */
  val helpFlag = Flag(
    name   = "help",
    flags  = Seq("-h", "--h", "--help"),
    help   = "Show this help message.")

  /** Minimize logging and other output. */
  val quiet = Flag(
    name  = "quiet",
    flags = Seq("-q", "--quiet"),
    help  = "Minimize output messages.")

  /** Create a String option */
  def string(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[String] = None) = apply(name, flags, help, default)(identity)

  /** Create a Char option. Just takes the first character in the value string. */
  def char(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Char] = None) = apply(name, flags, help, default)(_(0))

  /** Create a Byte option. */
  def byte(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Byte] = None) = apply(name, flags, help, default)(_.toByte)

  /** Create an Int option. */
  def int(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Int] = None) = apply(name, flags, help, default)(_.toInt)

  /** Create a Long option. */
  def long(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Long] = None) = apply(name, flags, help, default)(_.toLong)

  /** Create a Float option. */
  def float(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Float] = None) = apply(name, flags, help, default)(_.toFloat)

  /** Create a Double option. */
  def double(
    name:    String,
    flags:   Seq[String],
    help:    String = "",
    default: Option[Double] = None) = apply(name, flags, help, default)(_.toDouble)

  /**
   * Create an option where the value string represents a sequence with a delimiter.
   * The delimiter string is treated as a regex. For matching on several possible
   * delimiter characters, use "[;-_]", for example. The resulting substrings won't
   * be trimmed of whitespace, in case you want it, but you can also remove any
   * internal whitespace (i.e., not at the beginning or end of the input string),
   * e.g., "\\s*[;-_]\\s*".
   */
  def seq[V](
    delimsRE:  String,
    name:      String,
    flags:     Seq[String],
    help:      String = "",
    default:   Option[Seq[V]] = None)(fromString: String => V) = {
      require (delimsRE.trim.length > 0, "The delimiters RE string can't be empty.")
      apply(name, flags, help, default) (_.split(delimsRE) map fromString)
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
  help:    String = "",
  default: Option[V] = None)(fromString: String => V) extends Opt[V] {

  val parser: Opt.Parser[V] = {
    case flag +: value +: tail if flags.contains(flag) =>
      ((name, fromString(value)), tail)
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
      case flag +: tail if flags.contains(flag) => ((name, !default.get), tail)
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
