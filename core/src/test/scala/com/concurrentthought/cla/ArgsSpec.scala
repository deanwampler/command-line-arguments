package com.concurrentthought.cla
import scala.util.{Try, Success, Failure}
import org.scalatest.FunSpec
import java.io._

class ArgsSpec extends FunSpec {
  import SpecHelper._

  describe ("case class Args") {
    describe ("empty program invocation and comments") {
      it ("includes default values") {
        val args = Args.empty
        assert(args.programInvocation === Args.defaultProgramInvocation)
        assert(args.leadingComments   === Args.defaultComments)
        assert(args.trailingComments  === Args.defaultComments)
      }
    }
    describe ("nonempty program invocation and comments") {
      it ("includes the specified values") {
        val args = Args("programInvocation", "leadingComments", "trailingComments", Nil)
        assert(args.programInvocation === "programInvocation")
        assert(args.leadingComments   === "leadingComments")
        assert(args.trailingComments  === "trailingComments")
      }
    }
    describe ("empty list of options") {
      it ("still includes help") {
        val args = Args.empty.parse(Array("--help"))
        assert(args.programInvocation === Args.defaultProgramInvocation)
        assert(args.leadingComments   === Args.defaultComments)
        assert(args.trailingComments  === Args.defaultComments)
        val helpName = Args.helpFlag.name
        assert(args.opts.contains(Args.helpFlag))
        assert(args.defaults.contains(helpName))
        assert(args.values.contains(helpName))
        assert(args.allValues.contains(helpName))
      }

      it ("but the default help can be overridden, as long as the option has the name field \"help\".") {
        val altHelp = Opt.flag(
          name   = Args.HELP_KEY,
          flags  = Seq("-H", "--H", "--HELP"),
          help   = "Show this HELP message.")
        val args = Args(opts = Seq(altHelp))
        assert(args.opts.contains(altHelp))
        // before parsing:
        assert(args.defaults  === Map(Args.HELP_KEY -> false))
        assert(args.values    === Map(Args.HELP_KEY -> false))
        assert(args.allValues === Map(Args.HELP_KEY -> Vector(false)))
        // after parsing:
        val args2 = args.parse(Array("--HELP"))
        assert(args2.defaults  === Map(Args.HELP_KEY -> false))
        assert(args2.values    === Map(Args.HELP_KEY -> true))
        assert(args2.allValues === Map(Args.HELP_KEY -> Vector(true)))
      }

      it ("still includes an option for 'remaining' tokens not associated with flags") {
        val args = Args.empty
        val args2 = args.parse(Array("foo", "bar"))
        val remainingName = Args.remainingOpt.name
        Seq(args, args2) foreach { a =>
          assert(a.opts.contains(Args.remainingOpt))
          assert(a.defaults.contains(remainingName) === false)
          assert(a.values.contains(remainingName) === false)
          assert(a.allValues.contains(remainingName) === false)
        }
        assert(args.remaining  === Vector.empty[String])
        assert(args2.remaining === Vector("foo", "bar"))
      }
    }

    describe ("Zero or one option can have no flags.") {
      it ("an error is thrown if more than two `Opts` are given") {
        intercept[IllegalArgumentException] {
          Args(Seq(Opt.string("one", Nil), Opt.string("two", Nil)))
        }
        ()  // Suppress -Ywarn-value-discard warning
      }
    }

    describe ("before parsing") {
      it ("contains a map of the defined options with their default values") {
        val (args, values, allValues, remaining) = all
        assert(args.defaults  === allDefaults)
      }

      it ("contains maps of the values an 'all values' seen, which equal the default values") {
        val (args, values, allValues, remaining) = all
        assert(args.values     === values)
        assert(args.allValues  === allValues)
      }

      it ("contains the default values for the 'remaining' tokens, if any, which aren't associated with flags") {
        val (args, values, allValues, remaining) = all
        assert(args.remaining === remaining)
      }
    }

    describe ("after parsing") {

      it ("contains a map of the defined options, where each value is the LAST specified option value, or the default value") {
        val (args, values, allValues, remaining) = all
        assert(args.values === values)
      }

      it ("contains a map of the defined options, where each value has ALL the specified option values, or the default value") {
        val (args, values, allValues, remaining) = all
        assert(args.allValues === allValues)
        assert(args.failures === Nil)
      }

      it ("contains all the valid options matched and the failures for invalid options") {
        val args = Args(opts = allOpts).parse(Array("--foo", "bar", "--string", "hello", "-b"))
        val values = allDefaults + ("string" -> "hello")
        assert(args.values === values)
        assert(args.remaining === Vector("bar"))
        val failures = List(
          ("--foo", Args.UnrecognizedArgument("--foo", Seq("bar", "--string", "hello", "-b"))),
          ("-b",    Args.UnrecognizedArgument("-b", Nil)))
        assert(args.failures === failures)
      }

      it ("contains failures when argument values are not parseable to the correct type") {
        val args = Args(opts = allOpts).parse(Array(
          "--byte",   "z",
          "--char",   "",
          "--int",    "z",
          "--long",   "z",
          "--float",  "z",
          "--float",  "1_2",
          "--double", "z",
          "--double", "2_2",
          "--seq",    "a:b_c-d"))
        val failures = List(
          ("byte",   ivs("--byte",   "z",       Some(nfe("z")))),
          ("char",   ivs("--char",   "",        Some(new StringIndexOutOfBoundsException(0)))),
          ("int",    ivs("--int",    "z",       Some(nfe("z")))),
          ("long",   ivs("--long",   "z",       Some(nfe("z")))),
          ("float",  ivs("--float",  "z",       Some(nfe("z")))),
          ("float",  ivs("--float",  "1_2",     Some(nfe("1_2")))),
          ("double", ivs("--double", "z",       Some(nfe("z")))),
          ("double", ivs("--double", "2_2",     Some(nfe("2_2")))),
          ("seq",    ivs("--seq",    "a:b_c-d", Some(nfe("a")))))

        assert(args.values === args.defaults)
        assert(args.remaining === Vector.empty[String])
        args.failures zip failures foreach { case ((flag1,ex1), (flag2,ex2)) =>
          assert(flag1 === flag2)
          // Exception.equals doesn't work.
          assert(ex1.toString === ex2.toString)
        }
      }

      it ("contains values for successfully-parsed input and failures when the arguments couldn't be parsed, for repeated options") {
        val args = Args(opts = allOpts).parse(Array(
          "--byte",   "1",
          "--byte",   "z",
          "--byte",   "2"))
        val failures = List(
          ("byte",   ivs("--byte",   "z",       Some(nfe("z")))))
        assert(args.values    === (args.defaults + ("byte" -> 2)))
        assert(args.remaining === Vector.empty[String])
        assert(args.getAll[Byte]("byte") === Vector(1,2))
        args.failures zip failures foreach { case ((flag1,ex1), (flag2,ex2)) =>
          assert(flag1 === flag2)
          // Exception.equals doesn't work.
          assert(ex1.toString === ex2.toString)
        }
      }

      it ("contains a 'remaining' value for the non-flag, unrecognized arguments") {
        val args = Args.empty.parse(Array("a", "b"))
        assert(args.remaining === Vector("a", "b"))
        assert(args.values === Map(Args.HELP_KEY -> false))
        assert(args.allValues === Map(Args.HELP_KEY -> Vector(false)))
        assert(args.failures.isEmpty)
      }

      val req1 = Opt.string("req1", Seq("--r1"), requiredFlag = true)
      val req2 = Opt.string("req2", Seq("--r2"), requiredFlag = true, default = Some("foo"))
      val req3 = Opt.string("req3", Seq("--r3"), requiredFlag = true)
      val requiredArgs = Args(opts = Seq(req1, req2, req3))

      it ("contains a list of the required options, which are marked required AND don't have default values") {
        assert(requiredArgs.requiredOptions === Seq(req1, req3))
      }

      it ("returns failures for required options that weren't specified") {
        val args2 = requiredArgs.parse(Array.empty[String])
        assert(args2.failures === Seq(
          ("req1", Args.MissingRequiredArgument(req1)),
          ("req3", Args.MissingRequiredArgument(req3))))
      }

      def unknowns() = {
        val args = Args.empty.parse(Array("--foo", "-b", "bbb"))
        assert(args.values    === Map(Args.HELP_KEY -> false))
        assert(args.remaining === Vector("bbb"))
        val expected = List(
          ("--foo", Args.UnrecognizedArgument("--foo", Seq("-b", "bbb"))),
          ("-b",    Args.UnrecognizedArgument("-b", Seq("bbb"))))
        assert(args.failures  === expected)
        assert(args.remaining === Vector("bbb"))
      }
      it ("contains a list of the unknown flags in the user input") { unknowns() }
      it ("the 'values' doesn't contain the values specified with the bad options") { unknowns() }
      it ("the 'remains' does contain the values specified with the bad options") { unknowns() }

      it ("contains failures when an option is at the end of the list without a required value") {
        val args = Args(opts = allOpts)
        Seq("--byte", "--char", "--int", "--long", "--float", "--float", "--double", "--double", "--seq") foreach { flag =>
          val args2 = args.parse(Array(flag))
          assert(args2.defaults === allDefaults)
          assert(args2.values   === allDefaults)
          assert(args2.remaining === Vector.empty[String])
          assert(args2.failures === Seq((flag, Args.UnrecognizedArgument(flag, Nil))))
        }
      }
    }

    describe ("process())") {

      val unexpectedExit: Int => Unit = (n) => fail(s"Unexpected exit($n)")
      def expectedExit(expected: Int): Int => Unit = {
        (n) => assert(expected === n, s"Unexpected exit($n)")
        ()
      }

      it ("when successful, returns an Args with the updated Args") {
        val bytes = new ByteArrayOutputStream(2048)
        val out2 = new PrintStream(bytes, true)
        val args = Args(opts = allOpts).process(
          Array("--string", "hello"), out2, unexpectedExit)
        val values = allDefaults + ("string" -> "hello")
        assert(args.values === values)
        assert(args.remaining.size === 0)
        assert(args.failures.size === 0)
        assert(bytes.size === 0)
      }

      it ("when successful, but help requested returns a None and outputs the help") {
        val bytes = new ByteArrayOutputStream(2048)
        val out2 = new PrintStream(bytes, true)
        Args(opts = allOpts).process(
          Array("--help", "--string", "hello"), out2, expectedExit(0))
        assert(bytes.size !== 0)
      }

      it ("when unsuccessful, returns a None and outputs an error message") {
        val bytes = new ByteArrayOutputStream(2048)
        val out2 = new PrintStream(bytes, true)
        Args(opts = allOpts).process(
          Array("--bogus", "--string", "hello"), out2, expectedExit(1))
        assert(bytes.size !== 0)
      }
    }

    describe ("get[V]") {
      it ("returns an Option[V] for the flag, either the default or the last user-specified value.") {
        val (args, values, allValues, remaining) = all
        assert(args.get[String]("foo")      === None)
        assert(args.get[String]("string")   === Some("world!"))
        assert(args.get[Byte]("byte")       === Some(3))
        assert(args.get[Char]("char")       === Some('a'))
        assert(args.get[Int]("int")         === Some(4))
        assert(args.get[Long]("long")       === Some(5))
        assert(args.get[Float]("float")     === Some(1.1F))
        assert(args.get[Double]("double")   === Some(2.2))
        assert(args.get[Seq[Double]]("seq") === Some(Seq(111.3, 126.2, 123.4, 354.6)))
      }
    }

    describe ("getOrElse[V]") {
      it ("returns the found value or the default of type V for the flag or the second argument") {
        val (args, values, allValues, remaining) = all
        assert(args.getOrElse("string", "goodbye!")  === "world!")
        assert(args.getOrElse("string2", "goodbye!") === "goodbye!")
        assert(args.getOrElse("byte", 1)             === 3)
        assert(args.getOrElse("byte2", 1)            === 1)
        assert(args.getOrElse("seq",  Seq(1.1, 2.2)) === Seq(111.3, 126.2, 123.4, 354.6))
        assert(args.getOrElse("seq2", Seq(1.1, 2.2)) === Seq(1.1, 2.2))
      }
    }

    describe ("getAll[V]") {
      it ("returns a Seq[V] of the values for all invocations of the option, or the default value") {
        val (args, values, allValues, remaining) = all
        assert(args.getAll[String]("foo")      === Nil)
        assert(args.getAll[String]("string")   === Vector("hello", "world!"))
        assert(args.getAll[Byte]("byte")       === Vector(2,3))
        assert(args.getAll[Char]("char")       === Vector('a'))
        assert(args.getAll[Int]("int")         === Vector(4))
        assert(args.getAll[Long]("long")       === Vector(5))
        assert(args.getAll[Float]("float")     === Vector(1.1F))
        assert(args.getAll[Double]("double")   === Vector(2.2))
        assert(args.getAll[Seq[Double]]("seq") === Vector(Seq(111.3, 126.2, 123.4, 354.6)))
      }
      it ("returns Nil if there were no invocations of the option and the default value is None") {
        val (args, values, allValues, remaining) = all
        assert(args.getAll[String]("foo") === Nil)
      }
    }

    describe ("getOrElse[V]") {
      it ("returns a Seq[V] the values for all invocations of the option or the default value or the second argument") {
        val (args, values, allValues, remaining) = all
        assert(args.getAllOrElse("string", Seq("goodbye!"))  === Vector("hello", "world!"))
        assert(args.getAllOrElse("string2", Seq("goodbye!")) === Seq("goodbye!"))
        assert(args.getAllOrElse("byte", Seq(1))             === Vector(2,3))
        assert(args.getAllOrElse("byte2", Seq(1))            === Seq(1))
        assert(args.getAllOrElse("seq",  Seq(Seq(1.1, 2.2))) === Vector(Seq(111.3, 126.2, 123.4, 354.6)))
        assert(args.getAllOrElse("seq2", Seq(Seq(1.1, 2.2))) === Seq(Seq(1.1, 2.2)))
      }
    }

    describe ("toString") {
      val args = Args(
        programInvocation = "java foo",
        leadingComments   = "leading",
        trailingComments  = "trailing",
        opts              = allOpts)

      val tos = args.toString

      it ("contains the program invocation") {
        assert(tos.contains("program invocation: java foo"))
      }
      it ("contains the leading comments") {
        assert(tos.contains("leading comments: leading"))
      }
      it ("contains the trailing comments") {
        assert(tos.contains("trailing comments: trailing"))
      }
      it ("contains the options") {
        assert(tos.contains(s"opts: $allOpts"))
      }
      it ("contains the defaults") {
        assert(tos.contains(s"defaults: ${args.defaults}"))
      }
      it ("contains the values") {
        assert(tos.contains(s"values: ${args.values}"))
      }
      it ("contains all values") {
        assert(tos.contains(s"allValues: ${args.allValues}"))
      }
      it ("contains the remaining tokens") {
        assert(tos.contains(s"remaining: ${args.remaining}"))
      }
      it ("contains the failures") {
        assert(tos.contains(s"failures: ${args.failures}"))
      }
    }

    describe ("printValues") {
      it ("prints the default values before any parsing is done") {
        val args = Args(opts = allOpts)
        val out = new StringOut
        args.printValues(out.out)
        val expected = """
          |Command line arguments:
          |        help: false
          |        anti: true
          |      string: foobar
          |        byte: 0
          |        char: x
          |         int: 0
          |        long: 0
          |       float: 0.0
          |      double: 0.0
          |         seq: List()
          |  seq-string: List()
          |        path: List()
          |      others: Vector()
          |
          |""".stripMargin
        assert(out.toString === expected)
      }

      it ("prints the default values overridden by user-specified options (the last invocation of any one option...) after parsing is done") {
        val args = Args(opts = allOpts).parse(Array(
          "--help",
          "--anti",
          "--string",     "hello",
          "--byte",       "3",
          "--char",       "abc",
          "--int",        "4",
          "--long",       "5",
          "--float",      "1.1",
          "foo",
          "--double",     "2.2",
          "--seq",        "111.3:126.2_123.4-354.6",
          "--seq-string", "a:b_c-d",
          "bar",
          "--path",       s"/foo/bar${pathDelim}/home/me",
          "baz"))
        val out = new StringOut
        args.printValues(out.out)
        val expected = """
          |Command line arguments:
          |        help: true
          |        anti: false
          |      string: hello
          |        byte: 3
          |        char: a
          |         int: 4
          |        long: 5
          |       float: 1.1
          |      double: 2.2
          |         seq: Vector(111.3, 126.2, 123.4, 354.6)
          |  seq-string: Vector(a, b, c, d)
          |        path: Vector(/foo/bar, /home/me)
          |      others: Vector(foo, bar, baz)
          |
          |""".stripMargin
        assert(out.toString === expected)
      }
    }

    describe ("printAllValues") {
      it ("prints the default values before any parsing is done") {
        val args = Args(opts = allOpts)
        val out = new StringOut
        args.printAllValues(out.out)
        val expected = """
          |Command line arguments (all values given):
          |        help: Vector(false)
          |        anti: Vector(true)
          |      string: Vector(foobar)
          |        byte: Vector(0)
          |        char: Vector(x)
          |         int: Vector(0)
          |        long: Vector(0)
          |       float: Vector(0.0)
          |      double: Vector(0.0)
          |         seq: Vector(List())
          |  seq-string: Vector(List())
          |        path: Vector(List())
          |      others: Vector()
          |
          |""".stripMargin
        assert(out.toString === expected)
      }

      it ("prints the default values overridden by user-specified options after parsing is done") {
        val args = Args(opts = allOpts).parse(Array(
          "--help",
          "--anti",
          "--string",     "hello",
          "--byte",       "3",
          "--char",       "abc",
          "--int",        "4",
          "--long",       "5",
          "--float",      "1.1",
          "foo",
          "--double",     "2.2",
          "--seq",        "111.3:126.2_123.4-354.6",
          "--seq-string", "a:b_c-d",
          "bar",
          "--path",       s"/foo/bar${pathDelim}/home/me",
          "baz"))
        val out = new StringOut
        args.printAllValues(out.out)
        val expected = """
          |Command line arguments (all values given):
          |        help: Vector(true)
          |        anti: Vector(false)
          |      string: Vector(hello)
          |        byte: Vector(3)
          |        char: Vector(a)
          |         int: Vector(4)
          |        long: Vector(5)
          |       float: Vector(1.1)
          |      double: Vector(2.2)
          |         seq: Vector(Vector(111.3, 126.2, 123.4, 354.6))
          |  seq-string: Vector(Vector(a, b, c, d))
          |        path: Vector(Vector(/foo/bar, /home/me))
          |      others: Vector(foo, bar, baz)
          |
          |""".stripMargin
        assert(out.toString === expected)
      }
    }

    describe ("handleHelp") {
      it ("does nothing before any arguments have been parsed") {
        val args = Args(opts = allOpts)
        val out = new StringOut
        args.handleHelp(out.out)
        assert(out.toString.length === 0, out.toString)
      }
      it ("does nothing if help wasn't requested") {
        val args = Args(opts = allOpts).parse(Array[String]())
        val out = new StringOut
        args.handleHelp(out.out)
        assert(out.toString.length === 0, out.toString)
      }
      it ("prints the help message to the out PrintStream, if help is requested") {
        val args = Args(opts = allOpts).parse(Array("--help"))
        val out = new StringOut
        args.handleHelp(out.out)
        assert(out.toString.length > 0)
      }
    }

    describe ("handleErrors") {
      it ("does nothing before any arguments have been parsed") {
        val args = Args(opts = allOpts)
        val out = new StringOut
        args.handleErrors(out.out)
        assert(out.toString.length === 0, out.toString)
      }
      it ("does nothing if no parsing errors occurred") {
        val args = Args(opts = allOpts).parse(Array("--help"))
        val out = new StringOut
        args.handleErrors(out.out)
        assert(out.toString.length === 0, out.toString)
      }
      it ("prints the error and help messages to the out PrintStream if errors occurred") {
        val args = Args(opts = allOpts).parse(Array("--xxx"))
        val out = new StringOut
        args.handleErrors(out.out)
        assert(out.toString.length > 0)
      }
    }
  }

  private def nfe(s: String) = new NumberFormatException("For input string: \"%s\"".format(s))
  private def ivs(flag: String, value: String, ex: Option[RuntimeException]) =
    Opt.InvalidValueString(flag, value, ex)

  private def all: (Args, Map[String,Any], Map[String,Seq[Any]], Vector[String]) = {
    val args = Args(opts = allOpts).parse(Array(
      "--string",     "hello",
      "--string",     "world!",
      "--byte",       "2",
      "--byte",       "3",
      "--char",       "abc",
      "--int",        "4",
      "--long",       "5",
      "--float",      "1.1",
      "--double",     "2.2",
      "foo",
      "--seq",        "111.3:126.2_123.4-354.6",
      "--seq-string", "a:b_c-d",
      "--path",       s"/foo/bar${pathDelim}/home/me",
      "bar"))
    val values = Map[String,Any](
      Args.HELP_KEY -> false,
      "anti"        -> true,
      "string"      -> "world!",
      "byte"        ->   3,
      "char"        -> 'a',
      "int"         ->   4,
      "long"        ->   5,
      "float"       -> 1.1F,
      "double"      -> 2.2,
      "seq"         -> Seq(111.3, 126.2, 123.4, 354.6),
      "seq-string"  -> Vector("a", "b", "c", "d"),
      "path"        -> Vector("/foo/bar", "/home/me"))
    val allValues = Map[String,Seq[Any]](
      Args.HELP_KEY -> Vector(false),
      "anti"        -> Vector(true),
      "string"      -> Vector("hello", "world!"),
      "byte"        -> Vector(2,3),
      "char"        -> Vector('a'),
      "int"         -> Vector(4),
      "long"        -> Vector(5),
      "float"       -> Vector(1.1F),
      "double"      -> Vector(2.2),
      "seq"         -> Vector(Seq(111.3, 126.2, 123.4, 354.6)),
      "seq-string"  -> Vector(Vector("a", "b", "c", "d")),
      "path"        -> Vector(Vector("/foo/bar", "/home/me")))

    val remaining = Vector("foo", "bar")

    (args, values, allValues, remaining)
  }


  describe ("helpFlag") {
    it ("defines a help option") {
      assert(Args.helpFlag.name    === Args.HELP_KEY)
      assert(Args.helpFlag.default === Some(false))
    }
    it ("doesn't consume a value") {
      val result = Args.helpFlag.parser(Seq("--help", "one", "two"))
      assert((Args.HELP_KEY, Success(true)) === result._1)
      assert(Seq("one", "two") === result._2)
    }
  }

  describe ("quietFlag") {
    it ("defines a quiet option") {
      assert(Args.quietFlag.default === Some(false))
    }
    it ("doesn't consume a value") {
      val result = Args.quietFlag.parser(Seq("--quiet", "one", "two"))
      assert(("quiet", Success(true)) === result._1)
      assert(Seq("one", "two") === result._2)
    }
  }

  describe ("remainingOpt") {
    it ("defines a 'remaining' option for the command-line tokens not associated with flags") {
      assert(Args.remainingOpt.name    === Args.REMAINING_KEY)
      assert(Args.remainingOpt.flags   === Nil)
      assert(Args.remainingOpt.default === None)
    }
  }

  describe ("socketOpt") {
    it ("defines a socket (host:port) option") {
      assert(Args.socketOpt().default === None)
    }
    it ("can be made required") {
      assert(Args.socketOpt(required = true).required === true)
    }

    describe ("""requires a string of the form "host:port" option""") {
      it ("can be provided a default value") {
        assert(Args.socketOpt(default = Some(("host",123))).default === Some(("host",123)))
      }
      it ("succeeds if the host is a name or IP address and the port is an integer") {
        val expected = (("socket", Try(("host", 123))), Nil)
        assert(Args.socketOpt().parser(Seq("--socket", "host:123")) === expected)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the :port is missing") {
        Args.socketOpt().parser(Seq("--socket", "host"))
        val result = Args.socketOpt().parser(Seq("--socket", "host", "--bar"))
        val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "host", None)))
        val r2 = Seq("--bar")
        assert((r1, r2) === result)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the host: is missing") {
        val result = Args.socketOpt().parser(Seq("--socket", "123", "--bar"))
        val r1 = ("socket", Failure(Opt.InvalidValueString("--socket", "123", None)))
        val r2 = Seq("--bar")
        assert((r1, r2) === result)
      }
      it ("returns a Failure(Opt.InvalidValueString) if the port is not an integer") {
        Args.socketOpt().parser(Seq("--socket", "host:foo", "--bar")) match {
          case (("socket", Failure(failure)), Seq("--bar")) => failure match {
            case Opt.InvalidValueString("--socket", "host:foo (not an int?)", Some(th)) => /* pass */
            case _ => fail("Unexpected exception: "+failure)
          }
          case badResult => fail(badResult.toString)
        }
      }
    }
  }

  describe ("MissingRequiredArgument") {
    it ("handles required arguments that weren't provided") {
      val mra = Args.MissingRequiredArgument(SpecHelper.longOpt)
      assert(mra.toString.contains(
        """Missing required argument: "long" with flags -l | --l | --long, long help message"""))
    }
  }
}
