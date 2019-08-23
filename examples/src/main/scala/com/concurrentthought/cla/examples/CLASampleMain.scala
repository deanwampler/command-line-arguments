package com.concurrentthought.cla.examples
import com.concurrentthought.cla._

/**
 * Demonstrates how to use the API. Try running with different arguments,
 * including `--help`. Try running the following examples within SBT:
 * {{{
 * run-main com.concurrentthought.cla.examples.CLASampleMain -h
 * run -h
 * run --help
 * run -i /in -o /out -l 4 -p a:b --things x-y|z foo bar baz
 * run --in /in --out=/out -l=4 --path "a:b" --things=x-y|z -q foo bar baz
 * }}}
 * The last example demonstrates that both `argflag value` and `argflag=value`
 * syntax is supported. The "[...]" indicate optional arguments, so in this example,
 * you must specify the `input` argument and at least one token for "others".
 */
object CLASampleMain {

  def main(argstrings: Array[String]): Unit = {
    val initialArgs: Args = """
      |run-main CLASampleMain [options]
      |Demonstrates the CLA API.
      |   -i | --in  | --input      string              Path to input file.
      |  [-o | --out | --output     string=/dev/null]   Path to output file.
      |  [-l | --log | --log-level  int=3]              Log level to use.
      |  [-p | --path               path]               Path elements separated by ':' (*nix) or ';' (Windows).
      |  [--things                  seq([-|])]          String elements separated by '-' or '|'.
      |  [-q | --quiet              flag]               Suppress some verbose output.
      |                             others              Other arguments.
      |Note that --input and "others" are required.
      |""".stripMargin.toArgs

    val finalArgs: Args = initialArgs.process(argstrings.toIndexedSeq)

    // If here, successfully parsed the args and none where "--help" or "-h".
    showResults(finalArgs)
  }

  /** Functionally identical to `main`, but more verbose. */
  def main2(argstrings: Array[String]): Unit = {
    val input  = Opt.string(
      name     = "input",
      flags    = Seq("-i", "--in", "--input"),
      help     = "Path to input file.",
      required = true)
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
    val path = Opt.path(
      name     = "path",
      flags    = Seq("-p", "--path"))
    val things = Opt.seqString(delimsRE = "[-|]")(
      name     = "things",
      flags    = Seq("--things"),
      help     = "String elements separated by '-' or '|'.")
    val others = Args.makeRemainingOpt(
      name     = "others",
      help     = "Other arguments",
      required = true)

    val initialArgs = Args(
      "run-main CLASampleMain [options]",
      "Demonstrates the CLA API.",
      """Note that --input and "others" are required.""",
      Seq(input, output, logLevel, path, things, Args.quietFlag, others))
      .parse(argstrings.toIndexedSeq)

    val finalArgs: Args = initialArgs.process(argstrings.toIndexedSeq)
    showResults(finalArgs)
  }

  /**
   * Functionally identical to `main` and `main2`, but more verbose than `main`,
   * yet a little less verbose than `main2`.
   */
  def main3(argstrings: Array[String]): Unit = {
    import Opt._
    import Args._
    val initialArgs = Args(
      "run-main CLASampleMain [options]",
      "Demonstrates the CLA API.",
      """Note that --input and "others" are required.""",
      Seq(
        string("input",     Seq("-i", "--in", "--input"),      None,              "Path to input file.", true),
        string("output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
        int(   "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
        path(  "path",      Seq("-p", "--path"),               None),
        seqString("[:;]")(
               "things",    Seq("--things"),                   None,              "String elements separated by '-' or '|'."),
        Args.quietFlag,
        makeRemainingOpt(
               "others",                                                          "Other arguments", true)))

    val finalArgs: Args = initialArgs.process(argstrings.toIndexedSeq)
    showResults(finalArgs)
  }

  protected def showResults(parsedArgs: Args): Unit = {

    // Was quiet specified? If not, then write some stuff...
    if (parsedArgs.getOrElse("quiet", false)) {
      println("(... I'm being very quiet...)")
    } else {
      // Print all the default values or those specified by the user.
      parsedArgs.printValues()

      // Print all the values including repeats.
      parsedArgs.printAllValues()

      // Repeat the "other" arguments (not associated with flags).
      println("\nYou gave the following \"other\" arguments: " +
        parsedArgs.remaining.mkString(", "))

      // Extract values and use them. Note that an advantage of getOrElse is that
      // the type parameter for the function can be inferred. E.g., `[Int]` is
      // inferred here.
      showPathElements(parsedArgs.get[Seq[String]]("path"))
      showLogLevel(parsedArgs.getOrElse("log-level", 0))
      println
    }
  }

  protected def showPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to show!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def showLogLevel(level: Int) =
    println(s"New log level: $level")
}
