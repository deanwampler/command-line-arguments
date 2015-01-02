package com.concurrentthought.cla

object SampleOpts {

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
    name     = "seq",
    flags    = Seq("-s", "--s", "--seq"),
    default  = Some(Nil),
    help     = "seq help message")(Opt.toTry(_.toDouble))


  val seqStringOpt = Opt.seqString(delimsRE = "[-_:]")(
    name     = "seq-string",
    flags    = Seq("-ss", "--ss", "--seq-string"),
    default  = Some(Nil),
    help     = "seq-string help message")

  val pathOpt = Opt.path(
    name     = "path",
    flags    = Seq("-p", "--path"),
    default  = Some(Nil))

  val pathDelim = sys.props.getOrElse("path.separator",":")

  val allOpts = Seq(stringOpt, byteOpt, charOpt, intOpt, longOpt,
    floatOpt, doubleOpt, seqOpt, seqStringOpt, pathOpt)
  val allDefaults = allOpts.map(o => (o.name, o.default.get)).toMap + ("help" -> false)
}