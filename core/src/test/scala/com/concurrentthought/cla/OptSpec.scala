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
      ()  // Suppress -Ywarn-value-discard warning
    }

    it ("allows an empty help message") {
      // No exception thrown:
      Opt.string(
        name    = "in",
        flags   = Seq("-i", "--in", "--input"))
      ()  // Suppress -Ywarn-value-discard warning
    }

    it ("can be required") {
      val opt = Opt.string(
        name     = "in",
        flags    = Seq("-i", "--in", "--input"),
        required = true)
      assert(opt.isRequired === true)
    }

    it ("defaults to not required") {
      val opt = Opt.string(
        name     = "in",
        flags    = Seq("-i", "--in", "--input"))
      assert(opt.isRequired === false)
    }

    it ("ignores the required flag if the default is not None") {
      val opt = Opt.string(
        name     = "in",
        flags    = Seq("-i", "--in", "--input"),
        default  = Some("foo"),
        required = true)
      assert(opt.isRequired === false)
    }

    it ("allows the list of flags to be empty (but see Args requirements)") {
      // No exception thrown:
      Opt.string(
        name    = "in",
        flags   = Nil)
      ()  // Suppress -Ywarn-value-discard warning
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

    describe ("flag() constructs a Boolean option") {
      it ("defaults the value to false") {
        assert(helpFlag.default === Some(false))
      }

      it ("can be set to use true as the default value") {
        assert(antiFlag.default === Some(true))
      }

      it ("does not consume any values in the argument list.") {
        val result = helpFlag.parser(Seq("--help", "one", "two"))
        assert(Seq("one", "two")   === result._2)
      }

      it ("returns true if the option is used and the default was false.") {
        val result = helpFlag.parser(Seq("--help", "one", "two"))
        assert((Args.HELP_KEY, Success(true)) === result._1)
      }

      it ("returns false if the option is used and the default was true.") {
        val result = antiFlag.parser(Seq("--anti", "one", "two"))
        assert(("anti", Success(false)) === result._1)
      }
    }

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
          case ("char", Failure(Opt.InvalidValueString("--char", "", Some(ex@_)))) => /* pass */
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
            case Opt.InvalidValueString("--byte", "x", Some(th@_)) => /* pass */
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
            case Opt.InvalidValueString("--int", "x", Some(th@_)) => /* pass */
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
            case Opt.InvalidValueString("--long", "x", Some(th@_)) => /* pass */
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
            case Opt.InvalidValueString("--float", "x", Some(th@_)) => /* pass */
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
            case Opt.InvalidValueString("--double", "x", Some(th@_)) => /* pass */
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
            case Opt.InvalidValueString("--seq", "a:b_c-d", Some(th@_)) => /* pass */
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

    describe ("socket constructs a (String,Int) option for socket host:port combinations") {
      it ("defines a socket (host:port) option") {
        assert(Opt.socket().default === None)
      }
      it ("can be made required") {
        assert(Opt.socket(required = true).isRequired === true)
      }

      describe ("""requires a string of the form "host:port" option""") {
        it ("can be provided a default value") {
          assert(Opt.socket(default = Some(("host",123))).default === Some(("host",123)))
        }
        it ("succeeds if the host is a name or IP address and the port is an integer") {
          val expected = (("socket", Try(("host", 123))), Nil)
          assert(Opt.socket().parser(Seq("--socket", "host:123")) === expected)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the :port is missing") {
          Opt.socket().parser(Seq("--socket", "host"))
          val result = Opt.socket().parser(Seq("--socket", "host", "--bar"))
          val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "host", None)))
          val r2 = Seq("--bar")
          assert((r1, r2) === result)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the host: is missing") {
          val result = Opt.socket().parser(Seq("--socket", "123", "--bar"))
          val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "123", None)))
          val r2 = Seq("--bar")
          assert((r1, r2) === result)
        }
        it ("returns a Failure(Opt.InvalidValueString) if the port is not an integer") {
          Opt.socket().parser(Seq("--socket", "host:foo", "--bar")) match {
            case (("socket", Failure(failure)), Seq("--bar")) => failure match {
              case Opt.InvalidValueString("--socket", "host:foo (not an int?)", Some(th@_)) => /* pass */
              case _ => fail("Unexpected exception: "+failure)
            }
            case badResult => fail(badResult.toString)
          }
        }
      }
    }
  }
}
