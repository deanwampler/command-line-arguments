package com.concurrentthought.cla

/**
 * Demonstrates how to use the API. Try running with different arguments,
 * including `--help`. Try running the following examples within SBT:
 * {{{
 * run-main com.concurrentthought.cla.CLASampleMain -h
 * run -h
 * run --help
 * run -i /in -o /out -l 4 -p a:b --things x-y|z foo bar baz
 * run --in /in --out=/out -l=4 --path "a:b" --things=x-y|z foo bar baz
 * }}}
 * The last example demonstrates that both `flag value` and `flag=value` syntax
 * is supported.
 */
object CLASampleMain {

  def main(argstrings: Array[String]) = {
    val args: Args = """
      |run-main CLASampleMain [options]
      |Demonstrates the CLA API.
      |  -i | --in  | --input      string              Path to input file.
      |  -o | --out | --output     string=/dev/null    Path to output file.
      |  -l | --log | --log-level  int=3               Log level to use.
      |  -p | --path               path                Path elements separated by ':' (*nix) or ';' (Windows).
      |       --things             seq([-|])           String elements separated by '-' or '|'.
      |                            others              Other arguments.
      |""".stripMargin.toArgs

    process(args, argstrings)
  }

  /** Functionally identical to `main`, but more verbose. */
  def main2(argstrings: Array[String]) = {
    val input  = Opt.string(
      name     = "input",
      flags    = Seq("-i", "--in", "--input"),
      help     = "Path to input file.")
    val output = Opt.string(
      name     = "output",
      flags    = Seq("-o", "--out", "--output"),
      default  = Some("/dev/null"),
      help     = "Path to output file.")
    val logLevel = Opt.int(
      name     = "log-level",
      flags    = Seq("-l", "--log", "--log-level"),
      default  = Some(3),
      help     = "Log level to use.")
    val path = Opt.seqString(delimsRE = "[:;]")(
      name     = "path",
      flags    = Seq("-p", "--path"),
      help     = "Path elements separated by ':' (*nix) or ';' (Windows).")
    val others = Args.makeRemainingOpt(
      name     = "others",
      help     = "Other arguments")

    val args = Args("run-main CLASampleMain [options]", "Demonstrates the CLA API.",
      Seq(input, output, logLevel, path, others)).parse(argstrings)

    process(args, argstrings)
  }

  /**
   * Functionally identical to `main` and `main2`, but more verbose than `main`,
   * yet a little less verbose than `main2`.
   */
  def main3(argstrings: Array[String]) = {
    import Opt._
    import Args._
    val args = Args("run-main CLASampleMain [options]", "Demonstrates the CLA API.",
      Seq(
        string("input",     Seq("-i", "--in", "--input"),      None,              "Path to input file."),
        string("output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
        int(   "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
        seqString("[:;]")(
               "path",      Seq("-p", "--path"),               None,              "Path elements separated by ':' (*nix) or ';' (Windows)."),
        makeRemainingOpt(
               "others",                                                          "Other arguments")))

    process(args, argstrings)
  }

  // Another example:
  protected def process(args: Args, argstrings: Array[String]): Unit = {
    // Use the Args object to parse the user-specified arguments.
    val parsedArgs = args.parse(argstrings)

    // If errors occurred or help was requested, print the appropriate messages
    // and exit.
    if (parsedArgs.handleErrors()) sys.exit(1)
    if (parsedArgs.handleHelp())   sys.exit(0)

    // Print all the default values or those specified by the user.
    parsedArgs.printValues()

    // Print all the values including repeats.
    parsedArgs.printAllValues()

    // Extract values and use them. Note that an advantage of getOrElse is that
    // the type parameter for the function can be inferred. E.g., `[Int]` is
    // inferred here.
    setPathElements(parsedArgs.get[Seq[String]]("path"))
    setLogLevel(parsedArgs.getOrElse("log-level", 0))

    println("\nYou gave the following \"other\" arguments: " +
      parsedArgs.remaining.mkString(", "))
    println
  }

  protected def setPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to set!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def setLogLevel(level: Int) =
    println(s"Setting log level to $level")
}