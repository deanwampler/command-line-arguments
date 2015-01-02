package com.concurrentthought.cla
import org.scalatest.FunSpec

class ArgsSpec extends FunSpec {
  import SampleOpts._

  describe ("case class Args") {
    describe ("empty list of options") {
      it ("still includes help") {
        assert(Args.empty.parse(Array("--help")) ===
          Args(Args.defaultProgramInvocation, Args.defaultDescription,
            Nil, Map("help" -> false), Map("help" -> true)))
      }
      it ("but the default help can be overridden, as long as the option has the name field \"help\".") {
        val altHelp = Flag(
          name   = "help",
          flags  = Seq("-H", "--H", "--HELP"),
          help   = "Show this HELP message.")
        val args = Args(opts = Seq(altHelp))
        assert(args.opts === Seq(altHelp))
        // before parsing:
        assert(args === 
          Args(Args.defaultProgramInvocation, Args.defaultDescription,
            Seq(altHelp), Map("help" -> false), Map("help" -> false)))
        // after parsing:
        assert(args.parse(Array("--HELP")) ===
          Args(Args.defaultProgramInvocation, Args.defaultDescription,
            Seq(altHelp), Map("help" -> false), Map("help" -> true)))
      }
    }

    it ("contains a list of invalid options after parsing") {
      val args = Args.empty.parse(Array("--foo", "-b"))
      assert(args.values === Map("help" -> false))
      val expected = List(
        ("--foo", Args.UnrecognizedArgument("--foo", Seq("-b"))),
        ("-b",    Args.UnrecognizedArgument("-b", Nil)))
      assert(args.failures === expected)
    }

    it ("contains a map of the defined options with their default values") {
      val (args, values) = all
      assert(args.defaults === allDefaults)
    }

    it ("after parsing, contains a map of the defined options with their default or specified values") {
      val (args, values) = all
      assert(args.values === values)
      assert(args.failures === Nil)
    }

    it ("contains all the valid options matched and the failures for invalid options") {
      val args = Args(opts = allOpts).parse(Array("--foo", "--string", "hello", "-b"))
      val values = allDefaults + ("string" -> "hello")
      assert(args.values === values)
      val failures = List(
        ("--foo", Args.UnrecognizedArgument("--foo", Seq("--string", "hello", "-b"))),
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
      args.failures zip failures foreach { case ((flag1,ex1), (flag2,ex2)) =>
        assert(flag1 === flag2)
        // Exception.equals doesn't work.
        assert(ex1.toString === ex2.toString)
      }
    }

    it ("contains failures when an option is at the end of the list without a required value") {
      val args = Args(opts = allOpts)
      Seq("--byte", "--char", "--int", "--long", "--float", "--float", "--double", "--double", "--seq") foreach { flag =>
        val args2 = args.parse(Array(flag))
        assert(args2.defaults === allDefaults)
        assert(args2.values   === allDefaults)
        assert(args2.failures === Seq((flag, Args.UnrecognizedArgument(flag, Nil))))
      }
    }

    describe ("get[V]") {
      it ("returns an option of the correct type for the flag") {
        val (args, values) = all
        assert(args.get[String]("foo")      === None)
        assert(args.get[String]("string")   === Some("hello"))
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
      it ("returns the found value or the default of the correct type for the flag") {
        val (args, values) = all
        assert(args.getOrElse("string", "goodbye!")  === "hello")
        assert(args.getOrElse("string2", "goodbye!") === "goodbye!")
        assert(args.getOrElse("byte", 1)             === 3)
        assert(args.getOrElse("byte2", 1)            === 1)
        assert(args.getOrElse("seq",  Seq(1.1, 2.2)) === Seq(111.3, 126.2, 123.4, 354.6))
        assert(args.getOrElse("seq2", Seq(1.1, 2.2)) === Seq(1.1, 2.2))
      }
    }

    describe ("printValues") {
      it ("prints the default values before any parsing is done") {
        val args = Args(opts = allOpts)
        val out = new StringOut
        args.printValues(out.out)
        val expected = """Command line arguments:
          |        byte: 0
          |        char: x
          |      double: 0.0
          |       float: 0.0
          |        help: false
          |         int: 0
          |        long: 0
          |        path: List()
          |         seq: List()
          |  seq-string: List()
          |      string: foobar
          |""".stripMargin
        assert(out.toString === expected)
      }

      it ("prints the default values overridden by user-specified options after parsing is done") {
        val args = Args(opts = allOpts).parse(Array(
          "--help",
          "--string",     "hello",
          "--byte",       "3",
          "--char",       "abc",
          "--int",        "4",
          "--long",       "5",
          "--float",      "1.1",
          "--double",     "2.2",
          "--seq",        "111.3:126.2_123.4-354.6",
          "--seq-string", "a:b_c-d",
          "--path",       s"/foo/bar${pathDelim}/home/me"))
        val out = new StringOut
        args.printValues(out.out)
        val expected = """Command line arguments:
          |        byte: 3
          |        char: a
          |      double: 2.2
          |       float: 1.1
          |        help: true
          |         int: 4
          |        long: 5
          |        path: Vector(/foo/bar, /home/me)
          |         seq: Vector(111.3, 126.2, 123.4, 354.6)
          |  seq-string: Vector(a, b, c, d)
          |      string: hello
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
      it ("prints to the out PrintStream") {
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
      it ("prints to the out PrintStream if errors occurred") {
        val args = Args(opts = allOpts).parse(Array("--xxx"))
        val out = new StringOut
        args.handleErrors(out.out)
        assert(out.toString.length > 0)
      }
    }
  }

  private def nfe(s: String) = new NumberFormatException("For input string: \"%s\"".format(s))
  private def ivs(flag: String, value: String, ex: Option[RuntimeException] = None) = 
    Opt.InvalidValueString(flag, value, ex)

  private def all: (Args, Map[String,Any]) = {
    val args = Args(opts = allOpts).parse(Array(
      "--string",     "hello",
      "--byte",       "3",
      "--char",       "abc",
      "--int",        "4",
      "--long",       "5",
      "--float",      "1.1",
      "--double",     "2.2",
      "--seq",        "111.3:126.2_123.4-354.6",
      "--seq-string", "a:b_c-d",
      "--path",       s"/foo/bar${pathDelim}/home/me"))
    val values = Map[String,Any](
      "help"       -> false,
      "string"     -> "hello",
      "byte"       ->   3,
      "char"       -> 'a',
      "int"        ->   4,
      "long"       ->   5,
      "float"      -> 1.1F,
      "double"     -> 2.2,
      "seq"        -> Seq(111.3, 126.2, 123.4, 354.6),
      "seq-string" -> Vector("a", "b", "c", "d"),
      "path"       -> Vector("/foo/bar", "/home/me"))

    (args, values)
  }
}