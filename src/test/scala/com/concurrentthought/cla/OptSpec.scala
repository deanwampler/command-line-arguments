package com.concurrentthought.cla
import org.scalatest.FunSpec
import scala.util.{Try, Success, Failure}

class OptSpec extends FunSpec {
  import SpecHelper._

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

    it ("allows the list of flags to be empty (but see Args requirements)") {
      Opt.string(
        name    = "in",
        flags   = Nil)
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

    describe("parsing tokens") {
      it ("extracts the flag and (optional) value from the sequence") {
        val result = stringOpt.parser(Seq("--string", "foo", "one", "two"))
        assert(("string", Success("foo")) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("handles 'flag=value' or 'flag value' forms") {
        val result = stringOpt.parser(Seq("--string", "foo", "one", "two"))
        assert(("string", Success("foo")) === result._1)
        assert(Seq("one", "two") === result._2)
        val result2 = stringOpt.parser(Seq("--string=foo", "one", "two"))
        assert(("string", Success("foo")) === result2._1)
        assert(Seq("one", "two") === result2._2)
      }
      it ("converts the value to the expected type") {
        val result = intOpt.parser(Seq("--int", "1234", "one", "two"))
        assert(("int", Success(1234)) === result._1)
        assert(Seq("one", "two") === result._2)
      }
      it ("returns a Failure if the value can't be converted to the expected type") {
        val result = intOpt.parser(Seq("--int", "xyz", "one", "two"))
        val (name, Failure(ex)) = result._1
        assert("int" === name)
        ex match {
          case Opt.InvalidValueString("--int", "xyz", _) => /* okay */
          case _ => fail(ex.toString)
        }
        assert(Seq("one", "two") === result._2)
      }
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

    describe ("seqString() constructs a Seq[String] option") {
      it ("splits the string into a Seq[String]") {
        val result = seqStringOpt.parser(Seq("--seq-string", "111.3:126.2_123.4-354.6", "one", "two"))
        assert((("seq-string", Try(Seq("111.3", "126.2", "123.4", "354.6"))), Seq("one", "two")) === result)
      }
    }

    describe ("path() constructs a Seq[String] option or platform-specific path, like CLASSPATH") {
      it ("""splits the string into a Seq[String] using the delimiter given by sys.props.getOrElse("path.separator",":")""") {
        val path1 = Seq("/foo/bar", "/home/me")
        val path =
          if (pathDelim != ":") path1.map(s => "C:"+s).mkString(pathDelim)
          else path1.mkString(pathDelim)
        val expected = Try(path.split(pathDelim).toVector)
        val result = pathOpt.parser(Seq("--path", path, "one", "two"))
        assert((("path", expected), Seq("one", "two")) === result)
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
        name    = Args.HELP_KEY,
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
      val result = Args.helpFlag.parser(Seq("--help", "one", "two"))
      assert(Seq("one", "two")   === result._2)
    }

    it ("returns true if the option is used.") {
      val result = Args.helpFlag.parser(Seq("--help", "one", "two"))
      assert((Args.HELP_KEY, Success(true)) === result._1)
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