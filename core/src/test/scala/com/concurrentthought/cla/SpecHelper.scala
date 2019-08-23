package com.concurrentthought.cla

object SpecHelper {

  val noremains = Opt.REMAINING_KEY -> Vector.empty[String]

  val helpFlag = Opt.flag(
    name    = Args.HELP_KEY,
    flags   = Seq("-h", "--h", "--help"),
    help    = "help message")

  val antiFlag = Opt.notflag(
    name    = "anti",
    flags   = Seq("-a", "--anti"),
    help    = "anti help message")

  val stringOpt = Opt.string(
    name    = "string",
    flags   = Seq("-s", "--s", "--string"),
    default = Some("foobar"),
    help    = "string help message")

  val charOpt = Opt.char(
    name    = "char",
    flags   = Seq("-c", "--c", "--char"),
    default = Some('x'),
    help    = "string help message")

  val byteOpt = Opt.byte(
    name    = "byte",
    flags   = Seq("-b", "--b", "--byte"),
    default = Some(0),
    help    = "byte help message")

  val intOpt = Opt.int(
    name    = "int",
    flags   = Seq("-i", "--i", "--int"),
    default = Some(0),
    help    = "int help message")

  val longOpt = Opt.long(
    name    = "long",
    flags   = Seq("-l", "--l", "--long"),
    default = Some(0),
    help    = "long help message")

  val floatOpt = Opt.float(
    name    = "float",
    flags   = Seq("-f", "--f", "--float"),
    default = Some(0.0F),
    help    = "float help message")

  val doubleOpt = Opt.double(
    name    = "double",
    flags   = Seq("-d", "--d", "--double"),
    default = Some(0.0),
    help    = "double help message")

  val seqOpt = Opt.seq[Double](delimsRE = "[-_:]")(
    name       = "seq",
    flags      = Seq("-s", "--s", "--seq"),
    default    = Some(Nil),
    help       = "seq help message")(
    fromString = Opt.toTry(_.toDouble))


  val seqStringOpt = Opt.seqString(delimsRE = "[-_:]")(
    name     = "seq-string",
    flags    = Seq("-ss", "--ss", "--seq-string"),
    default  = Some(Nil),
    help     = "seq-string help message")

  val pathOpt = Opt.path(
    name     = "path",
    flags    = Seq("-p", "--path"),
    default  = Some(Nil))

  val othersOpt = Args.makeRemainingOpt(
    name     = "others",
    help     = "Other tokens")

  val pathDelim = sys.props.getOrElse("path.separator",":")

  protected val allOpts1 = Vector(helpFlag, antiFlag, stringOpt, byteOpt, charOpt,
    intOpt, longOpt, floatOpt, doubleOpt, seqOpt, seqStringOpt, pathOpt)
  val allOpts = allOpts1 :+ othersOpt
  val allDefaults = allOpts1.foldLeft(Map.empty[String,Any]) { (map, o) =>
    o.default match {
      case None => map
      case Some(d) => map + (o.name -> d)
    }
  }
  val allRemaining = Vector.empty[String]


  val argsStr = """
    |java -cp ... foo
    |Some description
    |and a second line.
    |   -i | --in  | --input       string              Path to input file.
    |  [-o | --out | --output      string=/dev/null]   Path to output file.
    |  [-l | --log | --log-level   int=3]              Log level to use.
    |  [-p | --path                path]               Path elements separated by ':' (*nix) or ';' (Windows).
    |        --things              seq([-|])           Path elements separated by '-' or '|'.
    |  [-q | --quiet               flag]               Suppress some verbose output.
    |  [-a | --anti                ~flag]              An "antiflag" (defaults to true).
    |                              [others]            Other stuff.
    |Comments after the options,
    |which can be multiple lines.
    |""".stripMargin

  val expectedOpts = Vector(Args.helpFlag,
    Opt.string( "input",     Seq("-i", "--in" , "--input"),     None,              "Path to input file.", true),
    Opt.string( "output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
    Opt.int   ( "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
    Opt.path  ( "path",      Seq("-p", "--path"),               None,              "Path elements separated by ':' (*nix) or ';' (Windows)."),
    Opt.seqString("""[-|]""")
              ( "things",    Seq("--things"),                   None,              "Path elements separated by '-' or '|'.", true),
    Opt.flag(   "quiet",     Seq("-q", "--quiet"),                                 "Suppress some verbose output."),
    Opt.notflag("anti",      Seq("-a", "--anti"),                                  "An \"antiflag\" (defaults to true)."),
    Args.makeRemainingOpt("others", "Other stuff."))

  val expectedDefaults = Map[String,Any](
    Args.HELP_KEY -> false,
    "quiet"       -> false,
    "anti"        -> true,
    "output"      -> "/dev/null",
    "log-level"   -> 3)

  val expectedValuesBefore = Map[String,Any](
    Args.HELP_KEY -> false,
    "quiet"       -> false,
    "anti"        -> true,
    "output"      -> "/dev/null",
    "log-level"   -> 3)


  val expectedAllValuesBefore =
    expectedValuesBefore map { case (k,v) => k -> Vector(v) }

  val expectedValuesAfter = Map[String,Any](
    Args.HELP_KEY -> false,
    "quiet"       -> true,
    "anti"        -> false,
    "input"       -> "/foo/bar",
    "output"      -> "/out/baz",
    "log-level"   -> 4,
    "path"        -> Vector("a", "b", "c"),
    "things"      -> Vector("a", "b", "c"))

 val expectedRemainingBefore = Vector.empty[String]
 val expectedRemainingAfter = Vector("one", "two", "three", "four")

  val expectedAllValuesAfter =
    expectedValuesAfter.map { case (k,v) => k -> Vector(v) }

}
