package com.concurrentthought.cla
import org.scalatest.FunSpec
import org.scalatest.prop.PropertyChecks
import org.scalacheck.Gen
import org.parboiled.scala.testing.ParboiledTest
import scala.language.existentials


class OptParserSpec extends FunSpec with PropertyChecks with ParboiledTest {

  val leadingChar = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_'
  val leadingChars = Gen.oneOf(leadingChar)
  val trailingChar = Gen.oneOf('-' +: leadingChar)
  val trailingStrings = Gen.listOf(trailingChar)

  val typesWithInit = Vector (
    ("string=foo",   OptParser.StringTypeElem("=foo"),  Some("foo")),
    ("byte=127",     OptParser.ByteTypeElem("=127"),    Some(127)),
    ("char=127",     OptParser.CharTypeElem("=127"),    Some('1')),
    ("int=256",      OptParser.IntTypeElem("=256"),     Some(256)),
    ("long=512",     OptParser.LongTypeElem("=512"),    Some(512)),
    ("float=1.2",    OptParser.FloatTypeElem("=1.2"),   Some(1.2F)),
    ("double=2.3",   OptParser.DoubleTypeElem("=2.3"),  Some(2.3)),
    ("seq(:)=a:b:c", OptParser.SeqTypeElem(":","(:)=a:b:c"),  Some("a:b:c")),   // yes, odd SeqTypeElem args
    ("path=a:b:c",   OptParser.PathTypeElem("=a:b:c"),  Some("a:b:c")))

  val flagsTypes = Vector (
    ("flag",   OptParser.FlagTypeElem("false"), Some(false)),
    ("~flag",  OptParser.FlagTypeElem("true"), Some(true)))

  val typesWithoutInit = Vector (
    ("string", OptParser.StringTypeElem(""), None),
    ("byte",   OptParser.ByteTypeElem(""), None),
    ("char",   OptParser.CharTypeElem(""), None),
    ("int",    OptParser.IntTypeElem(""), None),
    ("long",   OptParser.LongTypeElem(""), None),
    ("float",  OptParser.FloatTypeElem(""), None),
    ("double", OptParser.DoubleTypeElem(""), None),
    ("seq(:)", OptParser.SeqTypeElem(":","(:)"), None),  // yes, odd SeqTypeElem args
    ("path",   OptParser.PathTypeElem(""), None)) ++ flagsTypes

  val other = OptParser.RemainingElem("other")

  protected def forNames[L,R](
    mkStrs:   String => Seq[String])(
    expected: String => Either[L,R])(
    actual:   String => Either[L,R]) = {
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
    describe ("Name parser returns a name as an OptParser.StringElem") {
      it ("expects the name to begin with a number, letter, or '_' and zero or more trailing characters from the same set plus '-'") {
        forNames(s => Seq(s))(s => Right(OptParser.StringElem(s)))(s => OptParser.parseWithRule(s, OptParser.Name))
      }
    }
    describe ("Flag parser returns a '-name' or '--name' as an OptParser.FlagElem") {
      it ("expects the flag to begin with a '-' or '--' followed by a valid name") {
        forNames(s => Seq("-"+s) )(s => Right(OptParser.FlagElem("-"+s)) )(s => OptParser.parseWithRule(s, OptParser.Flag))
        forNames(s => Seq("--"+s))(s => Right(OptParser.FlagElem("--"+s)))(s => OptParser.parseWithRule(s, OptParser.Flag))
      }
    }
    describe ("Flags parser returns an OptParser.FlagsElem with flags") {
      it ("expects the sequence of flags separated by '|' and optional white space") {
        forNames{ s =>
          val c = s.charAt(0)
          Seq(s"  -$c|--$c |-$s| --$s ")
        }{ s =>
          val c = s.charAt(0)
          val l = List(OptParser.FlagElem("-"+c), OptParser.FlagElem("--"+c),
            OptParser.FlagElem("-"+s), OptParser.FlagElem("--"+s))
          Right(OptParser.FlagsElem(l))
        }{
          s => OptParser.parseWithRule(s, OptParser.Flags)
        }
      }
    }

    describe ("Type parser returns an OptParser.TypeElem") {
      it ("returns the corresponding type or name element when no initializer is specified") {
        typesWithoutInit foreach { case (flag, expectedElem, initVal) =>
          OptParser.parseWithRule(flag, OptParser.TypeAndInit) match {
            case Left(ex) => fail(ex)
            case Right(term) =>
              assert(term === expectedElem)
              assert(term.initialValue == initVal)
          }
        }
      }

      it ("returns the corresponding type or name element with an initializer, when specified") {
        typesWithInit foreach { case (flag, expectedElem, initVal) =>
          OptParser.parseWithRule(flag, OptParser.TypeAndInit) match {
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

    def makeFlagsAndTypesElem(flag: String, expectedTypeElem: OptParser.TypeElem[_]) = {
      val flag2 = if (flag == "~flag") "not-flag" else flag
      val flagsStr = toFlags(flag2)
      val flags = flagsStr.split("""\s*\|\s*""").map(_.trim).toList
      val s = s"$flagsStr  $flag"
      val flagsElem = OptParser.FlagsElem(flags map (s => OptParser.FlagElem(s)))
      (s, OptParser.FlagsAndTypeElem(flagsElem, expectedTypeElem))
    }

    describe ("FlagsAndType parser") {
      it ("returns a list of flags and the type indicator") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal) =>
            val (s, expected) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            OptParser.parseWithRule(s, OptParser.FlagsAndType) match {
              case Left(ex) => fail(ex)
              case Right(term) => assert(term === expected)
            }
        }
      }
      it ("rejects flag and ~flag with initializers") {
        Seq("", "~") foreach { prefix =>
          val f = s"[-f | --flag ${prefix}flag=foo]"
          OptParser.parseWithRule(f, OptParser.FlagsAndType) match {
            case Right(term) => fail(s"$f succeeded ($term), but should have failed!")
            case Left(ex) =>
              assert(ex.toString.contains(s"ParsingException: Invalid input: `$f'"))
          }
        }
      }
    }

    describe ("Opt parser") {
      it ("returns an option with a list of flags, the type indicator, and optional help") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal) =>
            val (s, expectedFTE) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            Seq("", "with help") foreach { help =>
              val expected = OptParser.OptElem(expectedFTE, help)
              OptParser.parseWithRule(s"$s $help", OptParser.Opt) match {
                case Left(ex) => fail(ex)
                case Right(term) => assert(term === expected)
              }
            }
          }
      }
      it ("accepts optional arguments enclosed in [...]") {
        typesWithoutInit ++ typesWithInit foreach {
          case (flag, expectedTypeElem, initVal) =>
            val (s, expectedFTE) = makeFlagsAndTypesElem(flag, expectedTypeElem)

            Seq("", "with help") foreach { help =>
              val expected = OptParser.OptElem(expectedFTE, help)
              OptParser.parseWithRule(s"[$s] $help", OptParser.Opt) match {
                case Left(ex) => fail(ex)
                case Right(term) => assert(term === expected)
              }
            }
          }
      }
    }
  }
}