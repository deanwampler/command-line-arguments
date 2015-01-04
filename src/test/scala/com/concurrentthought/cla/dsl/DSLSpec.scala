package com.concurrentthought.cla.dsl
import com.concurrentthought.cla._
import org.scalatest.FunSpec

class DSLSpec extends FunSpec {
  import SpecHelper._

  val argsStr = """
    |java -cp foo
    |Some description
    |and a second line.
    |  -i | --in  | --input      string              Path to input file.
    |  -o | --out | --output     string=/dev/null    Path to output file.
    |  -l | --log | --log-level  int=3               Log level to use.
    |  -p | --path               path                Path elements separated by ':' (*nix) or ';' (Windows).
    |       --things             seq([-|])           Path elements separated by '-' or '|'.
    |                            others              Other stuff.
    |""".stripMargin

  val expectedOpts = Vector(Args.helpFlag,
    Opt.string("input",     Vector("-i", "--in" , "--input"),     None,              "Path to input file."),
    Opt.string("output",    Vector("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
    Opt.int   ("log-level", Vector("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
    Opt.path  ("path",      Vector("-p", "--path"),               None,              "Path elements separated by ':' (*nix) or ';' (Windows)."),
    Opt.seqString("""[-|]""")
              ("things",    Vector("--things"),                   None,              "Path elements separated by '-' or '|'."),
    Args.makeRemainingOpt("others", "Other stuff."))

  val expectedDefaults = Map[String,Any](
    Args.HELP_KEY -> false,
    "output"      -> "/dev/null",
    "log-level"   -> 3)

  val expectedValuesBefore = Map[String,Any](
    Args.HELP_KEY -> false,
    "output"      -> "/dev/null",
    "log-level"   -> 3)

  val expectedAllValuesBefore =
    expectedValuesBefore map { case (k,v) => k -> Vector(v) }

  val expectedValuesAfter = Map[String,Any](
    Args.HELP_KEY -> false,
    "input"       -> "/foo/bar",
    "output"      -> "/out/baz",
    "log-level"   -> 4,
    "path"        -> Vector("a", "b", "c"),
    "things"      -> Vector("a", "b", "c"))

 val expectedRemainingBefore = Vector.empty[String]
 val expectedRemainingAfter = Vector("one", "two", "three", "four")

  val expectedAllValuesAfter =
    expectedValuesAfter.map { case (k,v) => k -> Vector(v) }

  describe ("DSL") {
    describe ("implicit class ToArgs") {
      import dsl._

      it ("converts a multi line string into an Args") {
        val args = argsStr.toArgs
        assert(args.programInvocation === "java -cp foo")
        assert(args.description       === "Some description and a second line.")
        checkBefore(args)
      }

      it ("constructs an Args that parses arguments like any other Args") {
        val args = argsStr.toArgs.parse(Array(
          "--input",     "/foo/bar",
          "--output",    "/out/baz",
          "--log-level", "4",
          "one", "two",
          "--path",      s"a${pathDelim}b${pathDelim}c",
          "--things",    "a-b|c",
          "three", "four"))
        assert(args.programInvocation === "java -cp foo")
        assert(args.description       === "Some description and a second line.")
        checkAfter(args)
      }
    }

    describe("The String Format") {
      describe ("starts with zero or more lines, with no leading spaces") {
        it ("uses a blank 'program invocation' and 'description' if no such lines appear") {
          val str = """
            |  -i | --in  | --input      string              Path to input file.
            |""".stripMargin
          val args = str.toArgs
          assert(args.programInvocation === "")
          assert(args.description       === "")
        }
        it ("uses the first such line as the 'program invocation'.") {
          val str = """
            |java -cp foo
            |  -i | --in  | --input      string              Path to input file.
            |""".stripMargin
          val args = str.toArgs
          assert(args.programInvocation === "java -cp foo")
          assert(args.description       === "")
        }
        it ("uses all subsequent lines as the 'description', joined together into one, space-separated line") {
          val str = """
            |java -cp foo
            |Some description
            |and a second line.
            |  -i | --in  | --input      string              Path to input file.
            |""".stripMargin
          val args = str.toArgs
          assert(args.programInvocation === "java -cp foo")
          assert(args.description       === "Some description and a second line.")
        }
      }

      describe ("it expects each option on a separate line, with leading whitespace") {
        it ("extracts the leading zero or more single and/or double '-' flags, separated by |") {
          val args = argsStr.toArgs
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

  protected def checkBefore(args: Args) = {
    // assert(args.opts === expectedOpts)
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults === expectedDefaults)
    assert(args.values === expectedValuesBefore)
    assert(args.allValues === expectedAllValuesBefore)
    assert(args.remaining === expectedRemainingBefore)
  }

  protected def checkAfter(args: Args) = {
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults === expectedDefaults)
    assert(args.values === expectedValuesAfter)
    assert(args.allValues === expectedAllValuesAfter)
    assert(args.remaining === expectedRemainingAfter)
  }
}