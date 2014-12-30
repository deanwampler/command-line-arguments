package com.concurrentthought.cla

object Help {
  /** Arbitrary maximum width for the descriptive string after the options. */
  val maxHelpWidth = 60
}

/**
 * Format a help message for the command-line invocation of a program. It has
 * these fields:
 * <ol>
 * <li>`programInvocation` - e.g., "java -cp ... Foo"</li>
 * <li>`description` - An optional description or additional message.</li>
 * </ol>
 * Note that the formatted help message will include the default values for the
 * options, when they are defined, but excluding `Flag`s where the default is always
 * true or false, depending on the sense of the option.
 */
case class Help(
  programInvocation: String = "java -cp ...",
  description: Option[String] = None) {

  /** Return the help string for the given `Args`. */
  def apply(args: Args): String = {
    val lines = Vector(
      s"Usage: $programInvocation [options]",
      description.getOrElse("")) ++
      errorsHelp(args) ++
      Vector("Where the supported options are the following:") ++ argsHelp(args)
    (for { s <- lines } yield s).mkString("", "\n", "\n")
  }

  protected def errorsHelp(args: Args): Vector[String] =
    if (args.failures.size == 0) Vector.empty
    else "The following parsing errors occurred:" +:
      args.failures.map{ case (flag, err) => s"  $err" }.toVector

  protected def argsHelp(args: Args): Vector[String] = {
    val strings = args.opts.map(a => (toFlagsHelp(a), toHelp(a)))
    val maxFlagLen = strings.unzip._1.maxBy(_.size).size
    val fmt = s"%-${maxFlagLen}s    %s"
    strings.foldLeft(Vector.empty[String]) {
      case (vect, (flags, hlp)) =>
        vect ++ hlp.zipWithIndex.map {
          case (h, i) => fmt.format(if (i==0) flags else "", h)
        }
    }
  }

  protected def toFlagsHelp(arg: Opt[_]): String =
    arg.flags.mkString("  ", " | ", "")

  protected def toHelp(arg: Opt[_]): Vector[String] = {
    val h = arg.help
    val hs = if (h.length > Help.maxHelpWidth) wrap(h) else Vector(h)
    arg.default match {
      case None => hs
      case Some(d) => d match {
        case b: Boolean => hs  // suppress!
        case _ => hs ++ Vector(s"(default: ${d})")
      }
    }
  }

  protected def wrap(s: String): Vector[String] = {
    val sb = new StringBuilder()
    val ws = """\s""".r
    s.foldLeft((Vector.empty[String], 0, "")) {
      // Hit whitespace? If so, are we within 4 pos. of the max?
      case ((vect, pos, string), ws()) if pos > (Help.maxHelpWidth - 4) =>
        (vect :+ string, 0, "")  // start new string!
      // Are we starting a new string, but parsing whitespace? Skip it.
      case ((vect, pos, ""), ws()) => (vect, pos, "")
      // Normal character or well within the max width.
      case ((vect, pos, string), c) => (vect, pos + 1, string :+ c)
    } match {
      case (vect, _, "") => vect
      case (vect, _, s)  => vect :+ s
    }
  }
}
