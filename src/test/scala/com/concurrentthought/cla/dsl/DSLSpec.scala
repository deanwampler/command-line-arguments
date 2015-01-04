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
    |       --things             seq([-\|])          Path elements separated by '-' or '|'.
    |                            args                Other stuff.
    |""".stripMargin

  val expectedOpts = Vector(Opt.helpFlag,
    Opt.string("input",     Vector("-i", "--in" , "--input"),     None,              "Path to input file."),
    Opt.string("output",    Vector("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
    Opt.int   ("log-level", Vector("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
    Opt.path  ("path",      Vector("-p", "--path"),               None,              "Path elements separated by ':' (*nix) or ';' (Windows)."),
    Opt.seqString("""[-\|]""")
              ("things",    Vector("--things"),                   None,              "Path elements separated by '-' or '|'."),
    Opt.string("remaining", Vector(),                             None,              "Other stuff."))

  val expectedDefaults = Map[String,Any](
    "help"      -> false,
    "output"    -> "/dev/null",
    "log-level" -> 3)

  val expectedValuesBefore = Map[String,Any](
    "help"      -> false,
    "output"    -> "/dev/null",
    "log-level" -> 3)

  val expectedAllValuesBefore =
    expectedValuesBefore map { case (k,v) => k -> Vector(v) }

  val expectedValuesAfter = Map[String,Any](
    "help"      -> false,
    "input"     -> "/foo/bar",
    "output"    -> "/out/baz",
    "log-level" -> 4,
    "path"      -> Vector("a", "b", "c"),
    "things"    -> Vector("a", "b", "c"),
    "remaining" -> Vector("one", "two", "three", "four"))

  val expectedAllValuesAfter =
    expectedValuesAfter.map { case (k,v) => k -> Vector(v) } + (
      "remaining" -> Vector("one", "two", "three", "four"))

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
  }

  protected def checkBefore(args: Args) = {
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults === expectedDefaults)
    assert(args.values === expectedValuesBefore)
    assert(args.allValues === expectedAllValuesBefore)
  }

  protected def checkAfter(args: Args) = {
    (args.opts zip expectedOpts) foreach { case (o, eo) => assert(o === eo) }
    assert(args.defaults === expectedDefaults)
    assert(args.values === expectedValuesAfter)
    assert(args.allValues === expectedAllValuesAfter)
  }
}