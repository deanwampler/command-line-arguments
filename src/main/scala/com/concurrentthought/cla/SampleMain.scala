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
      delimsRE = "[:/]",
      name     = "path",
      flags    = Seq("-p", "--path"),
      help     = "Path elements.")(_.toString)

    val args = Args(Seq(input, output, path, logLevel)).parse(argstrings)
    val help = Help("run-main SampleMain", Some("Demonstrates the CLA API."))
    if (args.handleHelp(help))   sys.exit(0)
    if (args.handleErrors(help)) sys.exit(1)
    args.printValues(help)

    setLogLevel(args.get[Int]("log-level"))
  }

  protected def setLogLevel(level: Option[Int]) = {}
}