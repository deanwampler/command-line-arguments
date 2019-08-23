package com.concurrentthought.cla
import org.scalatest.FunSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
// import org.parboiled.scala.testing.ParboiledTest

class OptParserSpec extends FunSpec with ScalaCheckPropertyChecks { //with ParboiledTest {
  import OptParser._
  import Elems._

  val leadingChar = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_'
  val leadingChars = Gen.oneOf(leadingChar)
  val trailingChar = Gen.oneOf('-' +: leadingChar)
  val trailingStrings = Gen.listOf(trailingChar)

  val typesWithInit = Vector (
    ("string=foo",   StringTypeElem("=foo"),  Some("foo")),
    ("byte=127",     ByteTypeElem("=127"),    Some(127)),
    ("char=127",     CharTypeElem("=127"),    Some('1')),
    ("int=256",      IntTypeElem("=256"),     Some(256)),
    ("long=512",     LongTypeElem("=512"),    Some(512)),
    ("float=1.2",    FloatTypeElem("=1.2"),   Some(1.2F)),
    ("double=2.3",   DoubleTypeElem("=2.3"),  Some(2.3)),
    ("seq(:)=a:b:c", SeqTypeElem(":","(:)=a:b:c"),  Some("a:b:c")),   // yes, odd SeqTypeElem args
    ("path=a:b:c",   PathTypeElem("=a:b:c"),  Some("a:b:c")))

  val flagsTypes = Vector (
    ("flag",   FlagTypeElem("false"), Some(false)),
    ("~flag",  FlagTypeElem("true"), Some(true)))

  val typesWithoutInit = Vector (
    ("string", StringTypeElem(""), None),
    ("byte",   ByteTypeElem(""), None),
    ("char",   CharTypeElem(""), None),
    ("int",    IntTypeElem(""), None),
    ("long",   LongTypeElem(""), None),
    ("float",  FloatTypeElem(""), None),
    ("double", DoubleTypeElem(""), None),
    ("seq(:)", SeqTypeElem(":","(:)"), None),  // yes, odd SeqTypeElem args
    ("path",   PathTypeElem(""), None)) ++ flagsTypes

  // From parsing SpecHelper.argsStr:
  val expectedElems = Vector(
    Right(OptElem(false,FlagsAndTypeElem(FlagsElem(List(FlagElem("-i"), FlagElem("--in"), FlagElem("--input"))), StringTypeElem("")), "Path to input file.")),
    Right(OptElem(true, FlagsAndTypeElem(FlagsElem(List(FlagElem("-o"), FlagElem("--out"), FlagElem("--output"))), StringTypeElem("=/dev/null")), "Path to output file.")),
    Right(OptElem(true, FlagsAndTypeElem(FlagsElem(List(FlagElem("-l"), FlagElem("--log"), FlagElem("--log-level"))), IntTypeElem("=3")), "Log level to use.")),
    Right(OptElem(true, FlagsAndTypeElem(FlagsElem(List(FlagElem("-p"), FlagElem("--path"))), PathTypeElem("")), "Path elements separated by ':' (*nix) or ';' (Windows).")),
    Right(OptElem(false,FlagsAndTypeElem(FlagsElem(List(FlagElem("--things"))), SeqTypeElem("[-|]","([-|])")), "Path elements separated by '-' or '|'.")),
    Right(OptElem(true, FlagsAndTypeElem(FlagsElem(List(FlagElem("-q"), FlagElem("--quiet"))), FlagTypeElem("false")), "Suppress some verbose output.")),
    Right(OptElem(true, FlagsAndTypeElem(FlagsElem(List(FlagElem("-a"), FlagElem("--anti"))), FlagTypeElem("true")), """An "antiflag" (defaults to true).""")),
    Right(OptElem(true, RemainingElem("others"), "Other stuff.")))

  def fail(message: String): Nothing = super.fail(message)

  protected def forNames[L,R](
    mkStrs:   String => Seq[String])(
    actual:   String => Either[L,R])(
    expected: String => Either[L,R]) = {
    forAll(leadingChars, trailingStrings) {
      (leading: Char, trailing: Seq[Char]) =>
        val s = (leading +: trailing).mkString("")
        for { s2 <- Seq(s, "_"+s); s3 <- mkStrs(s2) } {
          assert(actual(s3) === expected(s2))
          val three = s"--${s3.trim}"
          actual(three) match {
            case Right(_) => fail(s"Didn't fail with --$three")
            case Left(_) => // pass
          }
        }
    }
  }

  describe ("OptParser") {
    describe ("Name parser returns a name as an StringElem") {
      it ("expects the name to begin with a number, letter, or '_' and zero or more trailing characters from the same set plus '-'") {
        forNames(s => Seq(s))(s => parseWithRule(s, Name))(s => Right(StringElem(s)))
      }
    }
    describe ("Flag parser returns a '-name' or '--name' as an FlagElem") {
      it ("expects the flag to begin with a '-' or '--' followed by a valid name") {
        forNames(s => Seq("-"+s) )(s => parseWithRule(s, Flag))(s => Right(FlagElem("-"+s)) )
        forNames(s => Seq("--"+s))(s => parseWithRule(s, Flag))(s => Right(FlagElem("--"+s)) )
      }
    }
    describe ("Flags parser returns an FlagsElem with flags") {
      it ("expects the sequence of flags separated by '|' and optional white space") {
        forNames{ s =>
          val c = s.charAt(0)
          Seq(s"  -$c|--$c |-$s| --$s ")
        }{
          s => parseWithRule(s, Flags)
        }{ s =>
          val c = s.charAt(0)
          val l = List(FlagElem("-"+c), FlagElem("--"+c),
            FlagElem("-"+s), FlagElem("--"+s))
          Right(FlagsElem(l))
        }
      }
    }

    describe ("Type parser returns an TypeElem") {
      it ("returns the corresponding type or name element when no initializer is specified") {
        typesWithoutInit foreach { case (flag, expectedElem, initVal) =>
          parseWithRule(flag, TypeAndInit) match {
            case Left(ex) => fail(ex)
            case Right(term) =>
              assert(term === expectedElem)
              assert(term.initialValue == initVal)
          }
        }
      }

      it ("returns the corresponding type or name element with an initializer, when specified") {
        typesWithInit foreach { case (flag, expectedElem, initVal) =>
          parseWithRule(flag, TypeAndInit) match {
            case Left(ex) => fail(ex)
            case Right(term) =>
              assert(term === expectedElem)
              assert(term.initialValue == initVal, flag)
          }
        }
      }
    }

    def toFlags(flag: String) = {
      val c = flag(0)
      val s = flag.split("[=(]")(0)
      s"-$c| --$c| -$s|--$s"
    }

    def makeFlagsAndTypesElem(flag: String, expectedTypeElem: TypeElem[_]) = {
      val flag2 = if (flag == "~flag") "not-flag" else flag
      val flagsStr = toFlags(flag2)
      val flags = flagsStr.split("""\s*\|\s*""").map(_.trim).toList
      val s = s"$flagsStr  $flag"
      val flagsElem = FlagsElem(flags map (s => FlagElem(s)))
      (s, FlagsAndTypeElem(flagsElem, expectedTypeElem))
    }

    describe ("FlagsAndType parser") {
      it ("returns a list of flags and the type indicator") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal@_) =>
            val (s, expected) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            parseWithRule(s, FlagsAndType) match {
              case Left(ex) => fail(ex)
              case Right(term) => assert(term === expected)
            }
        }
      }
      it ("rejects flag and ~flag with initializers") {
        Seq("", "~") foreach { prefix =>
          val f = s"[-f | --flag ${prefix}flag=foo]"
          parseWithRule(f, FlagsAndType) match {
            case Right(term) => fail(s"$f succeeded ($term), but should have failed!")
            case Left(ex) =>
              assert(ex.toString.contains(s"ParsingException: Invalid input: `$f'"))
          }
        }
      }
    }

    describe ("Full Opt parser") {
      it ("returns an option with a list of flags, the type indicator, and optional help") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal@_) =>
            val (s, expectedFTE) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            Seq("", "with help") foreach { help =>
              val expected = OptElem(false, expectedFTE, help)
              OptParser.parse(s"$s $help") match {
                case Left(ex) => fail(ex)
                case Right(term) => assert(term === expected)
              }
            }
        }
      }
      it ("accepts optional arguments enclosed in [...]") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal@_) =>
            val (s, expectedFTE) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            Seq("", "with help") foreach { help =>
              val expected = OptElem(true, expectedFTE, help)
              OptParser.parse(s"[$s] $help") match {
                case Left(ex) => fail(ex)
                case Right(term) => assert(term === expected)
              }
            }
        }
      }
      it ("parses a 'remaining arguments' line") {
        Seq(
          (s"%s",             false, ""),
          (s"[%s]",            true, ""),
          (s"%s with help",   false, "with help"),
          (s"[%s] with help",  true, "with help")) foreach { case (fmt, bool, help) =>

          forNames(s => Seq(fmt.format(s)))(s => OptParser.parse(s))(
            s => Right(OptElem(bool, RemainingElem(s), help)))

        }
      }
      it ("parses each valid line it is fed from a real specification") {
        SpecHelper.argsStr.split("\n").filter(_.startsWith(" ")).toSeq.zip(expectedElems) foreach {
          case (line, expected) =>
            assert(OptParser.parse(line) === expected)
        }
      }
    }
  }
}
