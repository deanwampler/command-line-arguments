package com.concurrentthought
import scala.util.Try

/**
 * Package object that adds  a mini-DSL allowing the user to construct an `Args`
 * using using a multi-line string.
 * @note Experimental!
 */
package object cla {

  /**
   * Specify the command-line arguments using a single, multi-line string.
   * Note the following example:
   * {{{
   * import com.concurrentthought.cla._
   *
   * val args: Args = """
   *   |java -cp ... foo
   *   |Some description
   *   |and a second line.
   *   |  [-i | --in  | --input       string]             Path to input file.
   *   |  [-o | --out | --output      string=/dev/null]   Path to output file.
   *   |  [-l | --log | --log-level   int=3]              Log level to use.
   *   |   -p | --path                path                Path elements separated by ':' (*nix) or ';' (Windows).
   *   |  [-q | --quiet               flag]               Suppress some verbose output.
   *   |       [--things              seq([-|])]          String elements separated by '-' or '|'.
   *   |                              others              Other stuff.
   *   |Any additional help description lines,
   *   |which also have no leading whitespace.
   *   |""".stripMargin.toArgs
   * }}}
   * The format, as illustrated in the example, has the following requirements:
   * <ol>
   * <li>Zero or more leading and trailing lines without opening whitespace are
   *   interpreted as the "program invocation" string in the help message, 
   *   followed by zero or more description lines, which will be concatenated
   *   together (separated by whitespace) in the help message.</li>
   * <li>Each option appears on a line with leading whitespace.</li>
   * <li>Each option has one or more flags, separated by "|". As a special case,
   *   one option can have no flags. It is used to provide a help message for all other
   *   command-line tokens that aren't associated with flags (which will be stored
   *   in `Args.remaining`).
   *   Note that these tokens are handled whether or not you specify a line like
   *   this or not.</li>
   * <li>If the option is not required for the user to specify a value, the flags
   *   and name must be wrapped in "[...]". Specifying a default value is effectively
   *   the name as not required, but the main purpose is display the expected
   *   behavior to the user.</li>
   * <li>After the flags, the "middle column" describes the type of option and
   *   optionally a default value. The types correspond to several helper functions
   *   in `Opt`: `string`, `byte`, `char`, `int`, `long`, `float`, `double`, `path`,
   *   `seqString`, where the string "seq" is used, followed by a required
   *   `(delim)` suffix to specify the delimiter regex as shown in the example, and
   *   `flag` which indicates a boolean flag where no value is expected, but the
   *   flag's presence means `true` and absence means `false`.
   *   However, for a no-flag option, the value in this column is interpreted as a
   *   name for the option for the help message. This is the one case where the string
   *   isn't interpreted as a type specifier.
   *   The `=` indicates the default value to use, if present. Current limitations
   *   of the type specification include the following: (i) `Opt.seq[V]` isn't
   *   supported in this mechanism, (ii) default values can't be specified for
   *   the `path` or `seq` types, nor for the no-flag case (an implementation limitation).
   * <li>The remaining text is interpreted as the help string.</li>
   * </ol>
   *
   * @note This is an experimental feature. There are several known limitations:
   * <ol>
   * <li>It provides no way to use the predefined flags like `Args.quietFlag`,
   *   although `Args.helpFlag` and `Args.remainingOpt` are automatically added
   *   if the list of options doesn't explicitly define help and no-flag constructs.</li>
   * <li>The general case of `Opt.seq[V]` isn't supported, only `Opt.seqString`.</li>
   * <li>It needs to be tested on a lot more examples.</li>
   * </ol>
   */
  implicit class ToArgs(str: String) {

    import Elems._  // scalastyle:ignore

    def toArgs: Args = {
      val lines = str.split("\n").filter(_.length != 0).toVector

      // Partition the lines into the leading comments, the options (which have
      // leading whitespace), and the trailing comments.
      type VS = Vector[String]
      val  ve = Vector.empty[String]
      def split(lines: Vector[String], result: (VS,VS,VS)): (VS,VS,VS) = {
        val leadingWhitespace = """^\s+""".r
        val (l,o,t) = result
        lines match {
          case head +: tail => leadingWhitespace.findFirstMatchIn(head) match {
            case None if o.size == 0 => split(tail, (l :+ head, o, t))
            case None => split(tail, (l, o, t :+ head))
            case _ => split(tail, (l, o :+ head, t))
          }
          case _ => result
        }
      }
      val (leading, options, trailing) = split(lines, (ve,ve,ve))
      val opts = options map { line =>
        OptParser.parse(line) match {
          case Left(ex) => throw ParseError(ex.getMessage, ex)
          case Right(OptElem(optional: Boolean, re: RemainingElem, help: String)) =>
            Args.makeRemainingOpt(re.name, help, !optional)
          case Right(OptElem(optional: Boolean, fte: FlagsAndTypeElem, help: String)) =>
            val flagStrs = fte.flags.flags.map (_.flag)
            val name = flagStrs.last.replaceAll("^--?", "")
            toOpt(fte.typ, name, optional, flagStrs, help)
          case Right(r) => throw ParseError("Unexpected element: "+r)
        }
      }
      val (programInvocation, description) = leading.size match {
        case 0 => ("", "")
        case 1 => (leading(0), "")
        case n:Int => (leading(0), leading.slice(1, n).mkString(" "))
      }
      Args(programInvocation, description, trailing.mkString(" "), opts.toVector)
    }

    // scalastyle:off
    protected def toOpt(typeElem: TypeElem[_],
      name: String, optional: Boolean, 
      flags: Seq[String], help: String): Opt[_] = typeElem match {

      case e: FlagTypeElem   => 
        if (toBool(e.initialValue) == false) Opt.flag(name, flags, help, !optional)
        else Opt.notflag(name, flags, help, !optional)
      case e: StringTypeElem => Opt.string(name, flags, e.initialValue, help, !optional)
      case e: ByteTypeElem   => Opt.byte(  name, flags, e.initialValue, help, !optional)
      case e: CharTypeElem   => Opt.char(  name, flags, e.initialValue, help, !optional)
      case e: IntTypeElem    => Opt.int(   name, flags, e.initialValue, help, !optional)
      case e: LongTypeElem   => Opt.long(  name, flags, e.initialValue, help, !optional)
      case e: FloatTypeElem  => Opt.float( name, flags, e.initialValue, help, !optional)
      case e: DoubleTypeElem => Opt.double(name, flags, e.initialValue, help, !optional)
      case e: SeqTypeElem    => Opt.seq(e.delimiter)(
                                           name, flags, toInitSeq(e.initialValue, e.delimiter), help, !optional)(s => Try(s.toString))
      case e: PathTypeElem   => Opt.path(  name, flags, toInitSeq(e.initialValue, Opt.pathSeparator), help, !optional)
    }
    // scalastyle:on

    protected def toBool(o: Option[Boolean]): Boolean = o.getOrElse(false)

    protected def toInitSeq(init: Option[String], delim: String): Option[Seq[String]] = 
      init.map(_.split(delim).toSeq)
  }
}

package cla {
  case class ParseError(msg: String, cause: Throwable = null) // scalastyle:ignore
    extends RuntimeException(msg, cause)
}

