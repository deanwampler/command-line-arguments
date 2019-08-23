package com.concurrentthought.cla
import org.scalatest.FunSpec

class HelpSpec extends FunSpec {
  import SpecHelper._

  val inOpt = Opt.string(
    name    = "in",
    flags   = Seq("-i", "--in", "--input"),
    help    = "Input files with an extremely long help message that should be wrapped by Help so it doesn't run off the screen like it does in this test source file!",
    required = true)
  val outOpt = Opt.string(
    name    = "out",
    flags   = Seq("-o", "--o", "--out", "--output"),
    default = Some("/dev/null"),
    help    = "Output files.")
  val reqOpt1 = Opt.string(
    name    = "req1",
    flags   = Seq("-r1", "--r1", "--req1"),
    help    = "Required opt. 1",
    required = true)
  val reqOpt2 = Opt.string(
    name    = "req2",
    flags   = Seq("-r2", "--r2", "--req2"),
    default = Some("foo"),
    help    = "Required opt. 2 (not really required)",
    required = true)

  describe ("Help") {
    it ("returns a help string based on the command-line arguments") {
      val args = Args(
        "java HelpSpec",
        "A ScalaTest for the Help class.",
        "Trailing comments.",
        Seq(inOpt, outOpt, reqOpt1, reqOpt2, Args.quietFlag))
      val help = Help(args)
      assert(help ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for the Help class.
        |Where the supported options are the following:
        |
        |  [-h | --h | --help]                   Show this help message.
        |   -i | --in | --input  in              Input files with an extremely long help message that should
        |                                        be wrapped by Help so it doesn't run off the screen like it
        |                                        does in this test source file!
        |  [-o | --o | --out | --output  out]    Output files.
        |                                        (default: /dev/null)
        |   -r1 | --r1 | --req1  req1            Required opt. 1
        |  [-r2 | --r2 | --req2  req2]           Required opt. 2 (not really required)
        |                                        (default: foo)
        |  [-q | --quiet]                        Suppress some verbose output.
        |  [remaining]                           All remaining arguments that aren't associated with flags.
        |
        |You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required.
        |Trailing comments.
        |""".stripMargin)
    }

    def doOptionalArgs() = {
      val help = Help(Args("java HelpSpec", Nil))
      assert(help ===
        s"""Usage: java HelpSpec [options]
        |
        |Where the supported options are the following:
        |
        |  [-h | --h | --help]    Show this help message.
        |  [remaining]            All remaining arguments that aren't associated with flags.
        |
        |
        |
        |""".stripMargin)
    }

    it ("returns a help string even when help is the only command-line argument supported") {
      doOptionalArgs
    }
    it ("returns a help string with [...] around optional arguments") {
      doOptionalArgs
    }
    it ("defaults the 'remaining' arguments to optional") {
      doOptionalArgs
    }
    it ("the 'remaining' arguments can be specified explicitly to make them required") {
      val help = Help(Args("java HelpSpec", Seq(Args.makeRemainingOpt(required=true))))
      assert(help ===
        s"""Usage: java HelpSpec [options]
        |
        |Where the supported options are the following:
        |
        |  [-h | --h | --help]    Show this help message.
        |   remaining             All remaining arguments that aren't associated with flags.
        |
        |
        |
        |""".stripMargin)
    }

    it ("returns a help string that includes the error messages after parsing") {
      val args = Args(
        "java HelpSpec",
        "A ScalaTest for no user-defined options.",
        "Trailing comments.",
        Seq(intOpt))
        .parse(Seq("--foo", "--int", "x"))
      val help = Help(args)
      assert(help ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for no user-defined options.
        |The following parsing errors occurred:
        |  Unrecognized argument (or missing value): --foo (rest of arguments: --int x)
        |  Invalid value string: x for option --int (cause java.lang.NumberFormatException: For input string: "x")
        |Where the supported options are the following:
        |
        |  [-h | --h | --help]        Show this help message.
        |  [-i | --i | --int  int]    int help message
        |                             (default: 0)
        |  [remaining]                All remaining arguments that aren't associated with flags.
        |
        |You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required.
        |Trailing comments.
        |""".stripMargin)
    }
  }
}
