package com.concurrentthought

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
   *   |  -i | --in  | --input      string              Path to input file.
   *   |  -o | --out | --output     string=/dev/null    Path to output file.
   *   |  -l | --log | --log-level  int=3               Log level to use.
   *   |  -p | --path               path                Path elements separated by ':' (*nix) or ';' (Windows).
   *   |       --things             seq([-|])           String elements separated by '-' or '|'.
   *   |                            others              Other stuff.
   *   |""".stripMargin.toArgs
   * }}}
   * The format, as illustrated in the example, has the following requirements:
   * <ol>
   * <li>The leading lines without opening whitespace are interpreted as the
   *   "program invocation" string in the help message, followed by zero or more
   *   description lines, which will be concatenated together (separated by
   *   whitespace) in the help message.</li>
   * <li>Each option appears on a line with leading whitespace.</li>
   * <li>Each option has one or more flags, separated by "|". As a special case,
   *   one option can have no flags. It is used to provide a help message for all other
   *   command-line tokens that aren't associated with flags (which will be stored
   *   in `Args.remaining`).
   *   Note that these tokens are handled whether or not you specify a line like
   *   this or not.</li>
   * <li>After the flags, the "middle column" describes the type of option and
   *   optionally a default value. The types correspond to several helper functions
   *   in `Opt`: `string`, `byte`, `char`, `int`, `long`, `float`, `double`, `path`,
   *   and `seqString`, where the string "seq" is used, followed by a required
   *   `(delim)` suffix to specify the delimiter regex, as shown in the example.
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
    def toArgs: Args = {
      val lines = str.split("""\n""").filter(_.length != 0).toSeq
      val (programInvocation, description, optLines) = lines.partition(_(0) != ' ') match {
        case (Nil, seq) => ("", "", seq)
        case (head +: Nil,  seq) => (head, "", seq)
        case (head +: tail, seq) => (head, tail.mkString(" "), seq)
      }
      val opts = optLines map { s =>
        val flagRE = """\s*(--?\w\S*)\s*\|?""".r
        val s2 = s.trim
        val flags  = flagRE.findAllMatchIn(s2).map(_.toString).map {
          case flagRE(flag) => flag
        }.toVector
        val rest   = if (flags.size == 0) s2 else {
          val Array(_, rest) = s2.split(flags.last, 2)
          rest.trim
        }
        val Array (typ1, help) = rest.split("""\s+""",2)
        val (typ, default) = typ1.split("""=""",2) match {
          case Array(v,d) => (v, Some(d))
          case Array(v) => (v, None)
        }
        // Use the LAST flag name (without the leading "-") as the name.
        // For the case where there is no flag, use "remaining".
        val name = if (flags.size == 0) Args.REMAINING_KEY else flags.last.replaceAll("^--?", "")
        try {
          fromType(typ, name, flags, default, help)
        } catch {
          case InvalidType => throw InvalidTypeInArgsString(typ, str)
        }
      }
      Args(programInvocation, description, opts.toVector)
    }

    protected def fromType(typ: String,
      name: String, flags: Seq[String], default: Option[String], help: String): Opt[_] = {
      val seqRE = """seq\(([^)]+)\)""".r
      typ match {
        case "string"     => Opt.string(name, flags, default, help)
        case "byte"       => Opt.byte(  name, flags, default.map(_.toByte), help)
        case "char"       => Opt.char(  name, flags, default.map(_(0)), help)
        case "int"        => Opt.int(   name, flags, default.map(_.toInt), help)
        case "long"       => Opt.long(  name, flags, default.map(_.toLong), help)
        case "float"      => Opt.float( name, flags, default.map(_.toFloat), help)
        case "double"     => Opt.double(name, flags, default.map(_.toDouble), help)
        case seqRE(delim) => Opt.seqString(delim)(name, flags, default.map(s => Seq(s)), help)
        case "seq"        =>
          println("com.concurrentthought.cla.dsl: WARNING, bare 'seq' (vs. 'seq(delim)') found. Using Opt.path.")
          Opt.path(name, flags, default.map(s => Seq(s)), help)
        case "path"       => Opt.path(name, flags, default.map(s => Seq(s)), help)
        case name if (flags.size == 0) => Args.makeRemainingOpt(name, help)
        case _ => throw new InvalidTypeInArgsString(typ, "")
      }
    }

    case object InvalidType extends RuntimeException("")
    case class InvalidTypeInArgsString(arg: String, all: String)
      extends RuntimeException(s"Unknown type label $arg in argument string. All of string: $all")
  }
}

