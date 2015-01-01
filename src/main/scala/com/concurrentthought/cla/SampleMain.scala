import com.concurrentthought.cla._

/**
 * Demonstrates how to use the API. Try running with different arguments,
 * including `--help`.
 */
object SampleMain {
  def main(argstrings: Array[String]) = {
    import Opt._
    val args = Args("run-main SampleMain", "Demonstrates the CLA API.",
      Seq(
        string("input",     Seq("-i", "--in", "--input"),      None,              "Path to input file."),
        string("output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
        int(   "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
        seq[String]("[:;]")(
               "path",      Seq("-p", "--path"),               None,              "Path elements separated by ':' or ';'.")(toTry(_.toString))))

    process(args, argstrings)
  }

  // Another example:
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
    val path = Opt.seq[String](delimsRE = "[:;]")(
      name     = "path",
      flags    = Seq("-p", "--path"),
      help     = "Path elements separated by ':' or ';'.")(Opt.toTry(_.toString))

    val args = Args("run-main SampleMain", "Demonstrates the CLA API.",
      Seq(input, output, path, logLevel)).parse(argstrings)

    process(args, argstrings)
  }

  protected def process(args: Args, argstrings: Array[String]): Unit = {
    // Use the Args object to parse the user-specified arguments.
    val parsedArgs = args.parse(argstrings)

    // If errors occurred or help was requested, print the appropriate messages
    // and exit.
    if (parsedArgs.handleErrors()) sys.exit(1)
    if (parsedArgs.handleHelp())   sys.exit(0)

    // Print all the default values or those specified by the user.
    parsedArgs.printValues()

    // Extract values and use them. Note that an advantage of getOrElse is that
    // the type parameter for the function can be inferred. E.g., `[Int]` is
    // inferred here.
    setPathElements(parsedArgs.get[Seq[String]]("path"))
    setLogLevel(parsedArgs.getOrElse("log-level", 0))
  }

  protected def setPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to set!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def setLogLevel(level: Int) =
    println(s"Setting log level to $level")
}