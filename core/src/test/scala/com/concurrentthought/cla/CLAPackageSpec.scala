package com.concurrentthought.cla
import org.scalatest.FunSpec

class CLAPackageSpec extends FunSpec {
  import SpecHelper._

  describe ("package cla") {
    describe ("implicit class ToArgs") {

      it ("converts a multi line string into an Args") {
        val args = argsStr.toArgs
        assert(args.programInvocation  === "java -cp ... foo")
        assert(args.leadingComments    === "Some description and a second line.")
        assert(args.trailingComments   === "Comments after the options, which can be multiple lines.")
        checkBefore(args)
      }

      it ("constructs an Args that parses arguments like any other Args") {
        val args = argsStr.toArgs.parse(Seq(
          "--quiet",
          "--anti",
          "--input",     "/foo/bar",
          "--output",    "/out/baz",
          "--log-level", "4",
          "one", "two",
          "--path",      s"a${pathDelim}b${pathDelim}c",
          "--things",    "a-b|c",
          "three", "four"))
        assert(args.programInvocation  === "java -cp ... foo")
        assert(args.leadingComments    === "Some description and a second line.")
        assert(args.trailingComments   === "Comments after the options, which can be multiple lines.")
        checkAfter(args)
      }
    }

    val platforms = Seq(
      ("UNIX",    "\n",   "\\n"),
      ("Windows", "\r\n", "\\r\\n"))

    // Split the test args string using the system line separator, then
    // rejoin the array with the test line separator.
    def format(lineSeparator: String, argsStr: String): String =
      argsStr.split(sys.props("line.separator")).mkString(lineSeparator)

    describe("The String Format") {
      for ((platform, lineSep, lineSepStr) <- platforms) {
        describe (s"For $platform files with line separator $lineSepStr") {
          describe ("starts with zero or more lines, with no leading spaces") {
            it ("uses a blank 'program invocation' and 'comments' if no such lines appear") {
              val str = format(lineSep, """
                |  -i | --in  | --input      string              Path to input file.
                |""".stripMargin)
              val args = str.toArgs
              assert(args.programInvocation  === "")
              assert(args.leadingComments    === "")
              assert(args.trailingComments   === "")
            }
            it ("uses the first such line as the 'program invocation'") {
              val str = format(lineSep, """
                |java -cp ... foo
                |  -i | --in  | --input      string              Path to input file.
                |""".stripMargin)
              val args = str.toArgs
              assert(args.programInvocation  === "java -cp ... foo")
              assert(args.leadingComments    === "")
            }
            it ("uses all subsequent lines as the 'leading comments', joined together into one, space-separated line") {
              val str = format(lineSep, """
                |java -cp ... foo
                |Some description
                |and a second line.
                |  -i | --in  | --input      string              Path to input file.
                |""".stripMargin)
              val args = str.toArgs
              assert(args.programInvocation  === "java -cp ... foo")
              assert(args.leadingComments    === "Some description and a second line.")
              assert(args.trailingComments   === "")
            }
            it ("uses all trailing lines with no leading whitespace after the options as the 'trailing comments', joined together into one, space-separated line") {
              val str = format(lineSep, """
                |java -cp ... foo
                |Some description
                |and a second line.
                |  -i | --in  | --input      string              Path to input file.
                |Comments after the options,
                |which can be multiple lines.
                |""".stripMargin)
              val args = str.toArgs
              assert(args.programInvocation  === "java -cp ... foo")
              assert(args.leadingComments    === "Some description and a second line.")
              assert(args.trailingComments   === "Comments after the options, which can be multiple lines.")
            }
          }

          describe ("it expects each option on a separate line, with leading whitespace") {
            it ("extracts the leading zero or more single and/or double '-' flags, separated by |") {
              val args = format(lineSep, argsStr).toArgs
              val expectedFlags = Vector(
                Args.helpFlag.flags,
                Vector("-i", "--in" , "--input"),
                Vector("-o", "--out", "--output"),
                Vector("-l", "--log", "--log-level"),
                Vector("-p", "--path"),
                Vector("--things"))
              (args.opts.map(_.flags) zip expectedFlags) foreach { case (f, ef) => assert(f === ef) }
            }
          }
        }
      }
    }
  }

  protected def checkBefore(args: Args) = {
    // assert(args.opts === expectedOpts)
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults  === expectedDefaults)
    assert(args.values    === expectedValuesBefore)
    assert(args.allValues === expectedAllValuesBefore)
    assert(args.remaining === expectedRemainingBefore)
  }

  protected def checkAfter(args: Args) = {
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults  === expectedDefaults)
    assert(args.values    === expectedValuesAfter)
    assert(args.allValues === expectedAllValuesAfter)
    assert(args.remaining === expectedRemainingAfter)
  }
}
