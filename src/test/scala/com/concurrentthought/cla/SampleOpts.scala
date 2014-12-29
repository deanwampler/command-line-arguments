package com.concurrentthought.cla

object SampleOpts {

  val stringOpt = Opt.string(
    name    = "string",
    flags   = Seq("-s", "--s", "--string"),
    help    = "string help message",
    default = Some("foobar"))

  val charOpt = Opt.char(
    name    = "char",
    flags   = Seq("-c", "--c", "--char"),
    help    = "string help message",
    default = Some('x'))

  val byteOpt = Opt.byte(
    name    = "byte",
    flags   = Seq("-b", "--b", "--byte"),
    help    = "byte help message",
    default = Some(0))

  val intOpt = Opt.int(
    name    = "int",
    flags   = Seq("-i", "--i", "--int"),
    help    = "int help message",
    default = Some(0))

  val longOpt = Opt.long(
    name    = "long",
    flags   = Seq("-l", "--l", "--long"),
    help    = "long help message",
    default = Some(0))

  val floatOpt = Opt.float(
    name    = "float",
    flags   = Seq("-f", "--f", "--float"),
    help    = "float help message",
    default = Some(0.0F))

  val doubleOpt = Opt.double(
    name    = "double",
    flags   = Seq("-d", "--d", "--double"),
    help    = "double help message",
    default = Some(0.0))

  val seqOpt = Opt.seq[Double](
    delimsRE = "[-_:]",
    name     = "seq",
    flags    = Seq("-s", "--s", "--seq"),
    help     = "seq help message",
    default  = Some(Nil))(_.toDouble)

  val allOpts = Seq(stringOpt, charOpt, byteOpt, intOpt, longOpt, floatOpt, doubleOpt, seqOpt)
  val allDefaults = allOpts.map(o => (o.name, o.default.get)).toMap + ("help" -> false)
}