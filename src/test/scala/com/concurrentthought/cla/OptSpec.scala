package com.concurrentthought.cla
import org.scalatest.FunSpec

class OptSpec extends FunSpec {
  import SampleOpts._

  describe ("case class Opt") {
    it ("requires a non-empty name") {
      intercept [IllegalArgumentException] {
        Opt.string(
          name    = "",
          flags   = Seq("-i", "--in", "--input"),
          help    = "help message",
          default = Some("/data/input"))
      }
    }

    it ("allows an empty help message") {
      Opt.string(
        name    = "in",
        flags   = Seq("-i", "--in", "--input"))
    }

    it ("allows the value to default to None") {
      val opt = Opt.string(
        name    = "in",
        flags   = Seq("-i", "--in", "--input"),
        help    = "help message")
      assert(opt.default === None)
    }

    it ("accepts a non-None value") {
      assert(stringOpt.default === Some("foobar"))
    }

    it ("provides a parser to extract the option flag and (optional) value") {
      val result = stringOpt.parser(Seq("--string", "foo", "one", "two"))
      assert(("string", "foo") === result._1)
      assert(Seq("one", "two") === result._2)
    }
  }

  describe ("object Opt") {
    describe ("string() constructs a String option") {
      it ("returns the value string unmodified") {
        val result = stringOpt.parser(Seq("--string", "foo", "one", "two"))
        assert(("string", "foo") === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("char() constructs a Char option") {
      it ("returns the first character of the value string") {
        val result = charOpt.parser(Seq("--char", "foo", "one", "two"))
        assert(("char", 'f') === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("byte() constructs a Byte option") {
      it ("parses the string into a Byte") {
        val result = byteOpt.parser(Seq("--byte", "126", "one", "two"))
        assert(("byte", 126) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("int() constructs an Int option") {
      it ("parses the string into an Int") {
        val result = intOpt.parser(Seq("--int", "1000", "one", "two"))
        assert(("int", 1000) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("long() constructs a Long option") {
      it ("parses the string into a Long") {
        val result = longOpt.parser(Seq("--long", "100000000", "one", "two"))
        assert(("long", 100000000) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("float() constructs a Float option") {
      it ("parses the string into a Float") {
        val result = floatOpt.parser(Seq("--float", "126.1", "one", "two"))
        assert(("float", 126.1F) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("double() constructs a Double option") {
      it ("parses the string into a Double") {
        val result = doubleOpt.parser(Seq("--double", "126.2", "one", "two"))
        assert(("double", 126.2) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("seq[V]() constructs a Seq[V] option") {
      it ("parses the string into a Seq[V]") {
        val result = seqOpt.parser(Seq("--seq", "111.3:126.2_123.4-354.6", "one", "two"))
        assert("seq" === result._1._1)
        assert(Seq(111.3, 126.2, 123.4, 354.6) === result._1._2.toSeq)
        assert(Seq("one", "two") === result._2)
      }
    }
  }

  describe ("Flag") {
    it ("requires a non-empty name") {
      intercept [IllegalArgumentException] {
        Flag(
          name    = "",
          flags   = Seq("-h", "--h", "--help"),
          help    = "help message")
      }
    }

    it ("allows an empty help message") {
      Flag(
        name    = "help",
        flags   = Seq("-h", "--h", "--help"))
    }

    it ("defaults the value to Some(false)") {
      val flag = Flag(
        name    = "in",
        flags   = Seq("-i", "--in", "--input"),
        help    = "help message")
      assert(flag.default === Some(false))
    }

    it ("does not consume any values in the argument list.") {
      val flag = Flag(
        name    = "help",
        flags   = Seq("-h", "--h", "--help"),
        help    = "help message")
      val result = flag.parser(Seq("--help", "one", "two"))
      assert(Seq("one", "two")   === result._2)
    }

    it ("returns true if the option is used.") {
      val flag = Flag(
        name    = "help",
        flags   = Seq("-h", "--h", "--help"),
        help    = "help message")
      val result = flag.parser(Seq("--help", "one", "two"))
      assert(("help", true) === result._1)
    }

    describe ("Flag.reverseSense") {
      it ("supports a reverse sense option with Flag.reverseSense") {
        val flag = Flag.reverseSense(
          name    = "antihelp",
          flags   = Seq("-a", "--ah", "--antihelp"),
          help    = "anti help message")
        assert(flag.default === Some(true))
      }

      it ("does not consume any values in the argument list.") {
        val flag = Flag.reverseSense(
          name    = "antihelp",
          flags   = Seq("-a", "--ah", "--antihelp"),
          help    = "anti help message")
        val result = flag.parser(Seq("--antihelp", "one", "two"))
        assert(Seq("one", "two")   === result._2)
      }

      it ("returns false if the option is used.") {
        val flag = Flag.reverseSense(
          name    = "antihelp",
          flags   = Seq("-a", "--ah", "--antihelp"),
          help    = "anti help message")
        val result = flag.parser(Seq("--antihelp", "one", "two"))
        assert(("antihelp", false) === result._1)
      }
    }
  }
}