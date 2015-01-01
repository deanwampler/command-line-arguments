package com.concurrentthought.cla
import org.scalatest.FunSpec
import scala.util.{Try, Success, Failure}

class OptSpec extends FunSpec {
  import SampleOpts._

  describe ("case class Opt") {
    it ("requires a non-empty name") {
      intercept [IllegalArgumentException] {
        Opt.string(
          name    = "",
          flags   = Seq("-i", "--in", "--input"),
          default = Some("/data/input"),
          help    = "help message")
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
      assert(("string", Success("foo")) === result._1)
      assert(Seq("one", "two") === result._2)
    }
  }

  describe ("object Opt") {
    describe ("string() constructs a String option") {
      it ("returns the value string unmodified") {
        val result = stringOpt.parser(Seq("--string", "foo", "one", "two"))
        assert(("string", Success("foo")) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("accepts an empty value string") {
        val result = stringOpt.parser(Seq("--string", "", "one", "two"))
        assert(("string", Success("")) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("helpFlag") {
      it ("defines a help option") {
        assert(Opt.helpFlag.default === Some(false))
      }
      it ("doesn't consume a value") {
        val result = Opt.helpFlag.parser(Seq("--help", "one", "two"))
        assert(("help", Success(true)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("quietFlag") {
      it ("defines a quiet option") {
        assert(Opt.quietFlag.default === Some(false))
      }
      it ("doesn't consume a value") {
        val result = Opt.quietFlag.parser(Seq("--quiet", "one", "two"))
        assert(("quiet", Success(true)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("socketFlag") {
      it ("defines a socket (host:port) option") {
        assert(Opt.socketFlag.default === None)
      }

      describe ("""requires a string of the form "host:port" option""") {
        it ("succeeds if the host is a name or IP address and the port is an integer") {
          val expected = (("socket", Try(("host", 123))), Nil)
          assert(Opt.socketFlag.parser(Seq("--socket", "host:123")) === expected)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the :port is missing") {
          Opt.socketFlag.parser(Seq("--socket", "host"))
          val result = Opt.socketFlag.parser(Seq("--socket", "host", "--bar"))
          val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "host", None)))
          val r2 = Seq("--bar")
          assert((r1, r2) === result)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the host: is missing") {
          val result = Opt.socketFlag.parser(Seq("--socket", "123", "--bar"))
          val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "123", None)))
          val r2 = Seq("--bar")
          assert((r1, r2) === result)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the port is not an integer") {
          Opt.socketFlag.parser(Seq("--socket", "host:foo", "--bar")) match {
            case (("socket", Failure(failure)), Seq("--bar")) => failure match {
              case Opt.InvalidValueString("--socket", "host:foo (not an int?)", Some(th)) => /* pass */
              case _ => fail("Unexpected exception: "+failure)
            }
            case badResult => fail(badResult.toString)
          }
        }
      }
    }

    describe ("char() constructs a Char option") {
      it ("returns the first character of the value string") {
        val result = charOpt.parser(Seq("--char", "foo", "one", "two"))
        assert(("char", Success('f')) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("requires a non-empty value string") {
        val result = charOpt.parser(Seq("--char", "foo", "one", "two"))
        assert(("char", Success('f')) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value string is empty") {
        val result = charOpt.parser(Seq("--char", "", "one", "two"))
        result._1 match {
          case ("char", Failure(Opt.InvalidValueString("--char", "", Some(ex)))) => /* pass */
          case bad => fail(bad.toString)
        }
        assert(Seq("one", "two") === result._2)
      }
    }

    describe ("byte() constructs a Byte option") {
      it ("parses the string into a Byte") {
        val result = byteOpt.parser(Seq("--byte", "126", "one", "two"))
        assert(("byte", Success(126)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value is not an integer") {
        byteOpt.parser(Seq("--byte", "x", "--bar")) match {
          case (("byte", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--byte", "x", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }

    describe ("int() constructs an Int option") {
      it ("parses the string into an Int") {
        val result = intOpt.parser(Seq("--int", "1000", "one", "two"))
        assert(("int", Success(1000)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value is not an integer") {
        intOpt.parser(Seq("--int", "x", "--bar")) match {
          case (("int", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--int", "x", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }

    describe ("long() constructs a Long option") {
      it ("parses the string into a Long") {
        val result = longOpt.parser(Seq("--long", "100000000", "one", "two"))
        assert(("long", Success(100000000)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value is not an integer") {
        longOpt.parser(Seq("--long", "x", "--bar")) match {
          case (("long", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--long", "x", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }

    describe ("float() constructs a Float option") {
      it ("parses the string into a Float") {
        val result = floatOpt.parser(Seq("--float", "126.1", "one", "two"))
        assert(("float", Success(126.1F)) === result._1)
        assert(Seq("one", "two") === result._2)
        val result2 = floatOpt.parser(Seq("--float", "126", "one", "two"))
        assert(("float", Success(126F)) === result2._1)
        assert(Seq("one", "two") === result2._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value is not a number") {
        floatOpt.parser(Seq("--float", "x", "--bar")) match {
          case (("float", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--float", "x", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }

    describe ("double() constructs a Double option") {
      it ("parses the string into a Double") {
        val result = doubleOpt.parser(Seq("--double", "126.2", "one", "two"))
        assert(("double", Success(126.2)) === result._1)
        assert(Seq("one", "two") === result._2)
        val result2 = doubleOpt.parser(Seq("--double", "126", "one", "two"))
        assert(("double", Success(126F)) === result2._1)
        assert(Seq("one", "two") === result2._2)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value is not a number") {
        doubleOpt.parser(Seq("--double", "x", "--bar")) match {
          case (("double", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--double", "x", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }

    describe ("seq[V]() constructs a Seq[V] option") {
      it ("parses the string into a Seq[V]") {
        val result = seqOpt.parser(Seq("--seq", "111.3:126.2_123.4-354.6", "one", "two"))
        assert((("seq", Try(Seq(111.3, 126.2, 123.4, 354.6))), Seq("one", "two")) === result)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the value fails to parse") {
        seqOpt.parser(Seq("--seq", "a:b_c-d", "--bar")) match {
          case (("seq", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--seq", "a:b_c-d", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }
  }

  describe ("case class Flag") {
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
      val result = Opt.helpFlag.parser(Seq("--help", "one", "two"))
      assert(Seq("one", "two")   === result._2)
    }

    it ("returns true if the option is used.") {
      val result = Opt.helpFlag.parser(Seq("--help", "one", "two"))
      assert(("help", Success(true)) === result._1)
    }
  }

  describe ("object Flag") {
    val antihelp = Flag.reverseSense(
      name    = "antihelp",
      flags   = Seq("-a", "--ah", "--antihelp"),
      help    = "anti help message")

    describe ("reverseSense") {
      it ("supports a reverse sense option with Flag.reverseSense") {
        assert(antihelp.default === Some(true))
      }

      it ("does not consume any values in the argument list.") {
        val result = antihelp.parser(Seq("--antihelp", "one", "two"))
        assert(Seq("one", "two")   === result._2)
      }

      it ("returns false if the option is used.") {
        val result = antihelp.parser(Seq("--antihelp", "one", "two"))
        assert(("antihelp", Success(false)) === result._1)
      }
    }
  }
}