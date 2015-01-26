package com.concurrentthought.cla
import org.scalatest.FunSpec

class CLAPackageSpec extends FunSpec {
  import SpecHelper._

  describe ("package cla") {
    describe ("implicit class ToArgs") {

      it ("converts a multi line string into an Args") {
        val args = argsStr.toArgs
        assert(args.programInvocation === "java -cp ... foo")
        assert(args.description       === "Some description and a second line.")
        checkBefore(args)
      }

      it ("constructs an Args that parses arguments like any other Args") {
        val args = argsStr.toArgs.parse(Array(
          "--quiet",
          "--anti",
          "--input",     "/foo/bar",
          "--output",    "/out/baz",
          "--log-level", "4",
          "one", "two",
          "--path",      s"a${pathDelim}b${pathDelim}c",
          "--things",    "a-b|c",
          "three", "four"))
        assert(args.programInvocation === "java -cp ... foo")
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
            |java -cp ... foo
            |  -i | --in  | --input      string              Path to input file.
            |""".stripMargin
          val args = str.toArgs
          assert(args.programInvocation === "java -cp ... foo")
          assert(args.description       === "")
        }
        it ("uses all subsequent lines as the 'description', joined together into one, space-separated line") {
          val str = """
            |java -cp ... foo
            |Some description
            |and a second line.
            |  -i | --in  | --input      string              Path to input file.
            |""".stripMargin
          val args = str.toArgs
          assert(args.programInvocation === "java -cp ... foo")
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