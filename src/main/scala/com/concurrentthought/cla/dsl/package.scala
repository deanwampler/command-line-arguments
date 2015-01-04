package com.concurrentthought.cla

/**
 * Package object for a mini-DSL that allows you to specify the options just
 * using a multi-line string.
 * @note Experimental!
 */
package object dsl {

  /**
   * Specify the command-line arguments using a single, multi-line string.
   * Note the following example:
   * {{{
   * val args: Args = """
   *   |java -cp foo
   *   |Some description
   *   |and a second line.
   *   |  -i | --in  | --input      string              Path to input file.
   *   |  -o | --out | --output     string=/dev/null    Path to output file.
   *   |  -l | --log | --log-level  int=3               Log level to use.
   *   |  -p | --path               path                Path elements separated by ':' (*nix) or ';' (Windows).
   *   |       --things             seq([-\|])          Path elements separated by '-' or '|'.
   *   |                            args                Other stuff.
   *   |""".stripMargin.toArgs
   * }}}
   * @note This is an experimental feature. There are several known limitations:
   * <ol>
   * <li>It provides no way to use the predefined flags like `Opt.quietFlag`.</li>
   * <li>The general case of `Opt.seq[V]` isn't supported, only `Opt.seqString`.</li>
   * <li>It's somewhat fragile and easy to break.</li>
   * <li>It has been tested yet on many example.</li>
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
        val name = if (flags.size == 0) "remaining" else flags.last.replaceAll("^--?", "")
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
        case _ if (flags.size == 0) => Opt.string(Args.REMAINING_KEY, flags, default, help)
        case _ => throw new InvalidTypeInArgsString(typ, "")
      }
    }

    case object InvalidType extends RuntimeException("")
    case class InvalidTypeInArgsString(arg: String, all: String)
      extends RuntimeException(s"Unknown type label $arg in argument string. All of string: $all")
  }
}

