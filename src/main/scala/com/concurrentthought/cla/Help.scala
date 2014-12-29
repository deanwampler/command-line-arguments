package com.concurrentthought.cla

object Help {
  // Arbitrary maximum width for the descriptive string after the options.
  val maxHelpWidth = 60
}

/**
 * Format a help message for the command-line invocation of a program.
 * @param programInvocation: String         e.g., "java -cp ... Foo"
 * @param description:       Option[String] An optional description or additional message.
 * @note Default values for the arguments are shown, when defined, except for {@see Flag}s where the default is always true.
 */
case class Help(programInvocation: String, description: Option[String]) {

  def apply(args: Seq[Opt[_]]): String = {
    val lines = Seq(
      s"Usage: $programInvocation [options]",
      description.getOrElse(""),
      "Where the options are the following:") ++ argsHelp(Opt.helpFlag +: args)
    (for { s <- lines } yield s).mkString("", "\n", "\n")
  }

  protected def argsHelp(args: Seq[Opt[_]]): Seq[String] = {
    val strings = args.map(a => (toFlagsHelp(a), toHelp(a)))
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

  protected def toHelp(arg: Opt[_]): Seq[String] = {
    val h = arg.help
    val hs = if (h.length > Help.maxHelpWidth) wrap(h) else Seq(h)
    arg.default match {
      case None => hs
      case Some(d) => d match {
        case b: Boolean => hs  // suppress!
        case _ => hs ++ Seq(s"(default: ${d})")
      }
    }
  }

  protected def wrap(s: String): Seq[String] = {
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
