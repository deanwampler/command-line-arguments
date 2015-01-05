package com.concurrentthought.cla
import org.scalatest.FunSpec

class HelpSpec extends FunSpec {
  import SpecHelper._

  val inOpt = Opt.string(
    name    = "in",
    flags   = Seq("-i", "--in", "--input"),
    default = Some("/data/input"),
    help    = "Input files with an extremely long help message that should be wrapped by Help so it doesn't run off the screen like it does in this test source file!")
  val outOpt = Opt.string(
    name    = "out",
    flags   = Seq("-o", "--o", "--out", "--output"),
    default = Some("/dev/null"),
    help    = "Output files.")

  describe ("Help") {
    it ("returns a help string based on the command-line arguments") {
      val args = Args("java HelpSpec", "A ScalaTest for the Help class.",
                      Seq(inOpt, outOpt, Args.quietFlag))
      val help = Help(args)
      assert(help ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for the Help class.
        |Where the supported options are the following:
        |  -h | --h | --help              Show this help message.
        |  -i | --in | --input            Input files with an extremely long help message that should
        |                                 be wrapped by Help so it doesn't run off the screen like it
        |                                 does in this test source file!
        |                                 (default: /data/input)
        |  -o | --o | --out | --output    Output files.
        |                                 (default: /dev/null)
        |  -q | --quiet                   Suppress some verbose output.
        |  remaining                      All remaining arguments that aren't associated with flags.
        |""".stripMargin)
    }

    it ("returns a help string even when help is the only command-line argument supported") {
      assert(Help(Args("java HelpSpec", "", Nil)) ===
        s"""Usage: java HelpSpec [options]
        |
        |Where the supported options are the following:
        |  -h | --h | --help    Show this help message.
        |  remaining            All remaining arguments that aren't associated with flags.
        |""".stripMargin)
    }

    it ("returns a help string that includes the error messages after parsing") {
      val args = Args("java HelpSpec", "A ScalaTest for no user-defined options.", Seq(intOpt))
        .parse(Seq("--foo", "--int", "x"))
      assert(Help(args) ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for no user-defined options.
        |The following parsing errors occurred:
        |  UnrecognizedArgument (or missing value): --foo (rest of arguments: --int x)
        |  Invalid value string: x for option --int (cause: java.lang.NumberFormatException: For input string: "x")
        |Where the supported options are the following:
        |  -h | --h | --help    Show this help message.
        |  -i | --i | --int     int help message
        |                       (default: 0)
        |  remaining            All remaining arguments that aren't associated with flags.
        |""".stripMargin)
    }
  }
}