package com.concurrentthought.cla

/**
 * Format a help message for the command-line invocation of a program, based
 * on the `Args` object passed to `apply`.
 */
object Help {
  /** Arbitrary maximum width for the descriptive string after the options. */
  val maxHelpWidth = 60

  /**
   * Return the help string for the given `Args`.
   * Note that the formatted help message will include the default values for the
   * options, when they are defined.
   */
  def apply(args: Args): String = {
    val lines = Vector(s"Usage: ${args.programInvocation} [options]", args.description) ++
      errorsHelp(args) ++
      Vector("Where the supported options are the following:") ++
      argsHelp(args) :+ trailing(args)
    (for { s <- lines } yield s).mkString("", "\n", "\n")
  }

  protected def errorsHelp(args: Args): Vector[String] =
    if (args.failures.size == 0) Vector.empty
    else "The following parsing errors occurred:" +:
      args.failures.map{ case (flag, err) => s"  $err" }.toVector

  protected def argsHelp(args: Args): Vector[String] = {
    val strings = args.opts.map(o => (toFlagsHelp(o), toHelp(o)))
    val maxFlagLen  = strings.map(_._1).maxBy(_.size).size
    val fmt = s"%-${maxFlagLen}s    %s"
    strings.foldLeft(Vector.empty[String]) {
      case (vect, (flags, hlp)) =>
        vect ++ hlp.zipWithIndex.map {
          case (h, i) => fmt.format(if (i==0) flags else "", h)
        }
    }
  }

  protected def toFlagsHelp(opt: Opt[_]): String = {
    val prefix = "  "
    val valueName = opt match { 
      case f: Opt.Flag => ""
      case _ => opt.name
    }
    val s = opt.flags.mkString(" | ")
    val (pre, suf) = if (!opt.required) ("[", "]") else (" ", " ")
    if (s.trim.length == 0) prefix+pre+valueName+suf 
    else if (valueName.length > 0) prefix+pre+s+prefix+valueName+suf
    else prefix+pre+s+suf
  }

  protected def toHelp(opt: Opt[_]): Vector[String] = {
    val h = opt.help
    val hs = if (h.length > Help.maxHelpWidth) wrap(h) else Vector(h)
    opt.default match {
      case None => hs
      case Some(d) => d match {
        case b: Boolean => hs  // suppress!
        case _ => hs ++ Vector(s"(default: ${d})")
      }
    }
  }

  /**
   * Add a trailing message about the alternative syntax, but only if there are
   * actually options that have flags and values, i.e., that aren't `Flags` and
   * aren't the special case option for tokens without a flag.
   * A bit of a hack...
   */
  protected def trailing(args: Args): String =
    if (args.opts.exists(o => o.isInstanceOf[Opt.Flag] == false && o.flags != Nil)) {
      "You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required."
    } else ""

  protected def wrap(s: String): Vector[String] = {
    s.foldLeft((Vector.empty[String], 0, "")) {
      // Hit whitespace? If so, are we within 4 pos. of the max?
      case ((vect, pos, string), c) if pos > (Help.maxHelpWidth - 4) && c.isWhitespace =>
        (vect :+ string, 0, "")  // start new string!
      // Are we starting a new string, but parsing whitespace? Skip it.
      case ((vect, pos, ""), c) if c.isWhitespace => (vect, pos, "")
      // Normal character or well within the max width.
      case ((vect, pos, string), c) => (vect, pos + 1, string :+ c)
    } match {
      case (vect, _, "") => vect
      case (vect, _, s)  => vect :+ s
    }
  }
}
