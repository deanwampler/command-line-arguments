package com.concurrentthought.cla
import org.scalatest.FunSpec

class HelpSpec extends FunSpec {

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
      val quiet = Flag(
        name  = "quiet",
        flags = Seq("-q", "--quiet"),
        help  = "Suppress log messages.")

      val help = Help("java HelpSpec", Some("A ScalaTest for the Help class."))
      // println(help(Seq(in, out, quiet)))
      assert(help(Seq(in, out, quiet)) ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for the Help class.
        |Where the options are the following:
        |  -h | --h | --help              Show this help message.
        |  -i | --in | --input            Input files with an extremely long help message that should
        |                                 be wrapped by Help so it doesn't run off the screen like it
        |                                 does in this test source file!
        |                                 (default: /data/input)
        |  -o | --o | --out | --output    Output files.
        |                                 (default: /dev/null)
        |  -q | --quiet                   Suppress log messages.
        |""".stripMargin)
    }

    it ("returns a help string even when help is the only command-line argument supported") {
      val help = Help("java HelpSpec", Some("A ScalaTest for no user-defined options."))
      // println(help(Seq(in, out, quiet)))
      assert(help(Nil) ===
        s"""Usage: java HelpSpec [options]
        |A ScalaTest for no user-defined options.
        |Where the options are the following:
        |  -h | --h | --help    Show this help message.
        |""".stripMargin)
    }
  }
}