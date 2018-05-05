package com.concurrentthought.cla
import org.parboiled.scala._  // scalastyle:ignore
import org.parboiled.errors.{ErrorUtils, ParsingException}

/** A set of "Elements", used for convenient access from clients. */
object Elems {
  sealed trait Elem

  case class OptElem(optional: Boolean, flags_remaining: FlagsAndType_Or_RemainingElem, help: String) extends Elem

  abstract class FlagsAndType_Or_RemainingElem extends Elem
  case class  FlagsAndTypeElem(flags: FlagsElem, typ: TypeElem[_]) extends FlagsAndType_Or_RemainingElem
  case class  RemainingElem(name: String) extends FlagsAndType_Or_RemainingElem


  case class FlagsElem(flags: Seq[FlagElem]) extends Elem
  case class FlagElem(flag: String) extends Elem
  case class StringElem(text: String) extends Elem

  import scala.reflect.ClassTag

  abstract class TypeElem[T : ClassTag](val initialValueStr: String)(toT: String => T) extends Elem {
    val initialValue: Option[T] = try {
      if (initialValueStr.trim == "") None else Some(toT(initialValueStr))
    } catch {
      case scala.util.control.NonFatal(ex) =>
        throw new ParsingException(
          s"Invalid initial value string, '$initialValueStr' for type ${implicitly[ClassTag[T]]}. Cause: $ex",
          ex)
    }
  }

  case class  FlagTypeElem(   ivs: String) extends TypeElem[Boolean](removeEQ(ivs))(_.toBoolean)
  case class  StringTypeElem( ivs: String) extends TypeElem[String](removeEQ(ivs))(_.toString)
  case class  ByteTypeElem(   ivs: String) extends TypeElem[Byte](removeEQ(ivs))(_.toByte)
  case class  CharTypeElem(   ivs: String) extends TypeElem[Char](removeEQ(ivs))(_(0))
  case class  IntTypeElem(    ivs: String) extends TypeElem[Int](removeEQ(ivs))(_.toInt)
  case class  LongTypeElem(   ivs: String) extends TypeElem[Long](removeEQ(ivs))(_.toLong)
  case class  FloatTypeElem(  ivs: String) extends TypeElem[Float](removeEQ(ivs))(_.toFloat)
  case class  DoubleTypeElem( ivs: String) extends TypeElem[Double](removeEQ(ivs))(_.toDouble)

  case class  SeqTypeElem(delimiter: String, ivs: String)  extends TypeElem[String](toVS(ivs))(identity)
  case class  PathTypeElem(ivs: String)   extends TypeElem[String](removeEQ(ivs))(identity)

  private def removeEQ(s:String) =
    if (s.startsWith("=")) s.substring(1,s.length) else s

  // Because of the way the parse strings are passed to SeqTypeElem, the ivs
  // string includes the delimiter string, so we remove it here, then remove
  // the equals sign.
  private def toVS(s:String) = {
    val ary = s.split("=",2)
    if (ary.length == 2) removeEQ(ary(1)) else ""
  }
}

// scalastyle:off

/** Parse a line defining an option. */
object OptParser extends Parser {

  val knownTypes = Vector ("flag", "~flag", "string", "byte", "char", "int", "long", "float", "double", "seq", "path")

  import Elems._

  protected def whiteSpace = " \n\r\t\f"

  def Opt: Rule1[OptElem] = rule { OptionalOpt | RequiredOpt }

  def OptionalOpt: Rule1[OptElem] = rule { group(OptionalFlagsAndType_Or_Remaining ~
    optional(WhiteSpacePlus) ~ Help) ~~> ((ft: FlagsAndType_Or_RemainingElem, help: String) => OptElem(true, ft, help)) }
  def RequiredOpt: Rule1[OptElem] = rule { group(FlagsAndType_Or_Remaining ~
    optional(WhiteSpacePlus) ~ Help) ~~> ((ft: FlagsAndType_Or_RemainingElem, help: String) => OptElem(false, ft, help)) }

  def LeftBracket  = rule { "[" ~ WhiteSpaceStar }
  def RightBracket = rule { WhiteSpaceStar ~ "]" }

  def OptionalFlagsAndType_Or_Remaining: Rule1[FlagsAndType_Or_RemainingElem] = rule {
    group(LeftBracket ~ FlagsAndType_Or_Remaining ~ RightBracket) }

  def FlagsAndType_Or_Remaining: Rule1[FlagsAndType_Or_RemainingElem] = rule {
    (FlagsAndType | Remaining) }

  def FlagsAndType: Rule1[FlagsAndTypeElem] = rule {
    group(Flags ~ WhiteSpacePlus ~ TypeAndInit) ~~> ((f: FlagsElem, t: TypeElem[_]) => FlagsAndTypeElem(f,t)) }

  // def Remaining: Rule1[RemainingElem] = rule { RT ~ (WhiteSpacePlus | EOI) }
  // protected def RT = rule { Name ~~> (se => RemainingElem(se.text)) }
  def Remaining: Rule1[RemainingElem] = rule { Name ~~> (se => RemainingElem(se.text)) }

  def Flags: Rule1[FlagsElem] = rule { oneOrMore(Flag, separator = WhiteSpaceStar ~ "|" ~ WhiteSpaceStar) ~~> FlagsElem }
  def Flag: Rule1[FlagElem]   = rule { Flag2 ~> FlagElem }
  def Flag2 = rule { ("--" | "-") ~ N2 }

  def TypeAndInit: Rule1[TypeElem[_]] = rule {
    FlagType | NotFlagType |
    StringType | ByteType | CharType |
    IntType | LongType | FloatType | DoubleType |
    SeqType | PathType }

  def FlagType:     Rule1[FlagTypeElem]    = rule { "flag"   ~ push(FlagTypeElem("false")) }
  def NotFlagType:  Rule1[FlagTypeElem]    = rule { "~flag"  ~ push(FlagTypeElem("true"))  }
  def StringType:   Rule1[StringTypeElem]  = rule { "string" ~  InitialValue ~> StringTypeElem }
  def ByteType:     Rule1[ByteTypeElem]    = rule { "byte"   ~  InitialValue ~> ByteTypeElem }
  def CharType:     Rule1[CharTypeElem]    = rule { "char"   ~  InitialValue ~> CharTypeElem }
  def IntType:      Rule1[IntTypeElem]     = rule { "int"    ~  InitialValue ~> IntTypeElem }
  def LongType:     Rule1[LongTypeElem]    = rule { "long"   ~  InitialValue ~> LongTypeElem }
  def FloatType:    Rule1[FloatTypeElem]   = rule { "float"  ~  InitialValue ~> FloatTypeElem }
  def DoubleType:   Rule1[DoubleTypeElem]  = rule { "double" ~  InitialValue ~> DoubleTypeElem }

  def SeqType:   Rule1[SeqTypeElem]  = rule { "seq" ~ SeqDIV ~~> SeqTypeElem }
  def PathType:  Rule1[PathTypeElem] = rule { "path" ~ InitialValue ~> PathTypeElem }
  protected def SeqDIV = rule { group(Delim ~ InitialValue) ~> identity }

  def Help: Rule1[String] = rule { zeroOrMore(ANY) ~> identity }

  def InitialValue = rule { optional("=" ~ oneOrMore(noneOf(whiteSpace+"[]"))) }

  // Keep the trailing ")", for use in splitting "upstream".
  def Delim = rule { "(" ~ D2 ~ ")" }
  protected def D2: Rule1[String] = rule { oneOrMore(noneOf(")")) ~> identity }

  def Name  = rule { N2 ~> StringElem }
  protected def N2 = rule { LDU ~ zeroOrMore(LDU | "-")  }

  def LDU   = rule { Letter | Digit | "_" }
  def Letter = rule { "a" - "z" | "A" - "Z"  }
  def Digit = rule { "0" - "9" }

  def WhiteSpace     = rule { anyOf(whiteSpace) }
  def WhiteSpaceStar = rule { zeroOrMore(WhiteSpace) }
  def WhiteSpacePlus = rule { oneOrMore(WhiteSpace) }

  /** Parse a full option string */
  def parse(s: String): Either[ParsingException, OptElem] =
    parseWithRule(s, Opt)

  def parseWithRule[E](s: String, rule: Rule1[E]): Either[ParsingException, E] =
    try {
      val result: ParsingResult[E] = ReportingParseRunner(rule).run(s.trim)
      result.result match {
        case Some(x) => Right(x)
        case None => Left(new ParsingException(
          s"Invalid input: `${s}'\n  Error:" + ErrorUtils.printParseErrors(result)))
      }
    } catch {
      case pe: ParsingException => Left(pe)
    }
}

// scalastyle:on
