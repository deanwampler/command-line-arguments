package com.concurrentthought.cla

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
   * @param delimsRE: String   A regex for the delimiter(s).
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
 * A command line argument with an explicit value.
 * @param name:       String       Used as a map key in the returned options. Cannot be empty.
 * @param flags:      Seq[String]  The flags marking the option. One or two "-" required for each.
 * @param help:       String       Help string displayed if user asks for help. Can be empty.
 * @param default:    Option[V]    Use as the default, unless None is used.
 * @param fromString: String => V  Convert the found value from a String.
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
 * by the user, then "false" is indicated. However, you can flip the sense of
 * the flag by calling `Flag.reverseSense(...)`.
 * @param name:       String       Used as a map key in the returned options. Cannot be empty.
 * @param flags:      Seq[String]  The flags marking the option. One or two "-" required for each.
 * @param help:       String       Help string displayed if user asks for help. Can be empty.
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

object Flag {
  def reverseSense(
  name:    String,
  flags:   Seq[String],
  help:    String = "") = new Flag(name, flags, help) {
    override val default = Some(true)
  }
}
