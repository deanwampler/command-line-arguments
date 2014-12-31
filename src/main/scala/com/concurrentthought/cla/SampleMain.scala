import com.concurrentthought.cla._
import scala.util.control.NonFatal
import java.io.PrintStream

/**
 * Demonstrates how to use the API. Try running with different arguments,
 * including `--help`.
 */
object SampleMain {
  def main(argstrings: Array[String]) = {
    val input  = Opt.string(
      name     = "input",
      flags    = Seq("-i", "--in", "--input"),
      help     = "Path to input file.")
    val output = Opt.string(
      name     = "output",
      flags    = Seq("-o", "--out", "--output"),
      help     = "Path to output file.",
      default  = Some("/dev/null"))
    val logLevel = Opt.int(
      name     = "log-level",
      flags    = Seq("-l", "--log", "--log-level"),
      help     = "Log level to use.",
      default  = Some(3))
    val path = Opt.seq[String](
      delimsRE = "[:;]",
      name     = "path",
      flags    = Seq("-p", "--path"),
      help     = "Path elements.")(Opt.toTry(_.toString))

    val args = Args(Seq(input, output, path, logLevel)).parse(argstrings)
    val help = Help("run-main SampleMain", "Demonstrates the CLA API.")
    if (args.handleErrors(help)) sys.exit(1)
    if (args.handleHelp(help))   sys.exit(0)
    args.printValues(help)

    setPathElements(args.get[Seq[String]]("path"))
    setLogLevel(args.getOrElse("log-level", 0))
  }

  protected def setPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to set!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def setLogLevel(level: Int) =
    println(s"Setting log level to $level")
}