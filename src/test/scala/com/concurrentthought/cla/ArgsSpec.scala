package com.concurrentthought.cla
import org.scalatest.FunSpec

class ArgsSpec extends FunSpec {
  import SampleOpts._

  describe ("case class Args") {
    describe ("empty list of options") {
      it ("still supports help") {
        assert(Args(Nil).parse(Array("--help")) ===
          Args(Nil, Map("help" -> false), Map("help" -> true)))
      }
    }

    it ("contains a list of invalid options after parsing") {
      val args = Args(Nil).parse(Array("--foo", "-b"))
      assert(args.values === Map("help" -> false))
      val expected = List(
        ("--foo", Args.UnrecognizedArgument("--foo", Seq("-b"))),
        ("-b",    Args.UnrecognizedArgument("-b", Nil)))
      assert(args.failures === expected)
    }
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
    val args = Args(allOpts).parse(Array("--foo", "--string", "hello", "-b"))
    val values = allDefaults + ("string" -> "hello")
    assert(args.values === values)
    val failures = List(
      ("--foo", Args.UnrecognizedArgument("--foo", Seq("--string", "hello", "-b"))),
      ("-b",    Args.UnrecognizedArgument("-b", Nil)))
    assert(args.failures === failures)
  }

  it ("contains failures when argument values are not parseable to the correct type") {
    val args = Args(allOpts).parse(Array(
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
      ("--byte",   nfe("z")),
      ("--char",   new StringIndexOutOfBoundsException(0)),
      ("--int",    nfe("z")),
      ("--long",   nfe("z")),
      ("--float",  nfe("z")),
      ("--float",  nfe("1_2")),
      ("--double", nfe("z")),
      ("--double", nfe("2_2")),
      ("--seq",    nfe("a")))

    assert(args.values === args.defaults)
    args.failures zip failures foreach { case ((flag1,ex1), (flag2,ex2)) =>
      assert(flag1 === flag2)
      // Exception.equals doesn't work.
      assert(ex1.toString === ex2.toString)
    }
  }

  it ("contains failures when an option is at the end of the list without a required value") {
    val args = Args(allOpts)
    Seq("--byte", "--char", "--int", "--long", "--float", "--float", "--double", "--double", "--seq") foreach { flag =>
      val args2 = args.parse(Array(flag))
      assert(args2.defaults === allDefaults)
      assert(args2.values   === allDefaults)
      assert(args2.failures === Seq((flag, Args.UnrecognizedArgument(flag, Nil))))
    }
  }

  describe ("get[V] returns an option of the correct type for the flag") {
    val (args, values) = all
    assert(args.get[String]("string")   === Some("hello"))
    assert(args.get[Byte]("byte")       === Some(3))
    assert(args.get[Char]("char")       === Some('a'))
    assert(args.get[Int]("int")         === Some(4))
    assert(args.get[Long]("long")       === Some(5))
    assert(args.get[Float]("float")     === Some(1.1F))
    assert(args.get[Double]("double")   === Some(2.2))
    assert(args.get[Seq[Double]]("seq") === Some(Seq(111.3, 126.2, 123.4, 354.6)))
  }

  private def nfe(s: String) = new NumberFormatException("For input string: \"%s\"".format(s))

  private def all: (Args, Map[String,Any]) = {
    val args = Args(allOpts).parse(Array(
      "--string", "hello",
      "--byte",   "3",
      "--char",   "abc",
      "--int",    "4",
      "--long",   "5",
      "--float",  "1.1",
      "--double", "2.2",
      "--seq",    "111.3:126.2_123.4-354.6"))
    val values = Map[String,Any](
      "help"   -> false,
      "string" -> "hello",
      "byte"   ->   3,
      "char"   -> 'a',
      "int"    ->   4,
      "long"   ->   5,
      "float"  -> 1.1F,
      "double" -> 2.2,
      "seq"    -> Seq(111.3, 126.2, 123.4, 354.6))
    (args, values)
  }
}