package com.concurrentthought.cla
import org.scalatest.FunSpec

class HelpSpec extends FunSpec {
  import SampleOpts._

  describe ("Help") {
    it ("returns a help string based on the command-line arguments") {
      val in = Opt[String](
        name    = "in",
        flags   = Seq("-i", "--in", "--input"),
        help    = "Input files with an extremely long help message that should be wrapped by Help so it doesn't run off the screen like it does in this test source file!",
        default = Some("/data/input"))(identity)
      val out = Opt[String](
        name    = "out",
        flags   = Seq("-o", "--o", "--out", "--output"),
        help    = "Output files.",
        default = Some("/dev/null"))(identity)
      val args = Args(Seq(in, out, Opt.quiet))
      val help = Help("java HelpSpec", Some("A ScalaTest for the Help class."))

      assert(help(args) ===
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
        |  -q | --quiet                   Minimize output messages.
        |""".stripMargin)
    }

    it ("returns a help string even when help is the only command-line argument supported") {
      val help = Help("java HelpSpec", Some("A ScalaTest for no user-defined options."))
      assert(help(Args(Nil)) ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for no user-defined options.
        |Where the supported options are the following:
        |  -h | --h | --help    Show this help message.
        |""".stripMargin)
    }

    it ("returns a help string that includes the error messages after parsing") {
      val help = Help("java HelpSpec", Some("A ScalaTest for no user-defined options."))
      val args = Args(Seq(intOpt)).parse(Seq("--foo", "--int", "x"))
      assert(help(args) ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for no user-defined options.
        |The following parsing errors occurred:
        |  UnrecognizedArgument (or missing value): --foo (rest of arguments: --int x)
        |  java.lang.NumberFormatException: For input string: "x"
        |Where the supported options are the following:
        |  -h | --h | --help    Show this help message.
        |  -i | --i | --int     int help message
        |                       (default: 0)
        |""".stripMargin)
    }
  }
}