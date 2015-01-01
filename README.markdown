## Command Line Arguments

Dean Wampler, Ph.D.
[@deanwampler](https://twitter.com/deanwampler)

This is a [Scala](http://scala-lang.org) library for handling command-line arguments. It has no dependencies on other libraries (other than [ScalaTest](http://scalatest.org)), so its footprint is small.

Details on defining releases as dependencies in SBT are TBD.

## Usage

The included [com.concurrentthought.cla.CLASampleMain](src/main/scala/com/concurrentthought/cla/CLASampleMain.scala) shows two different idiomatic ways to set up and use the API.

```scala
import com.concurrentthought.cla._

object CLASampleMain {

  def main(argstrings: Array[String]) = {
    val input  = Opt.string(
      name     = "input",
      flags    = Seq("-i", "--in", "--input"),
      help     = "Path to input file.")
    val output = Opt.string(
      name     = "output",
      flags    = Seq("-o", "--out", "--output"),
      default  = Some("/dev/null"),
      help     = "Path to output file.")
    val logLevel = Opt.int(
      name     = "log-level",
      flags    = Seq("-l", "--log", "--log-level"),
      default  = Some(3),
      help     = "Log level to use.")
    val path = Opt.seqString(delimsRE = "[:;]")(
      name     = "path",
      flags    = Seq("-p", "--path"),
      help     = "Path elements separated by ':' or ';'.")

    val args = Args("run-main CLASampleMain", "Demonstrates the CLA API.",
      Seq(input, output, logLevel, path)).parse(argstrings)

    process(args, argstrings)
  }
  ...
}
```

 Each option is defined using a [com.concurrentthought.cla.Opt](src/main/scala/com/concurrentthought/cla/Opt.scala) value. In this case, there are helper methods in the `Opt` companion object for constructing options where the values are strings or numbers. The `string` and `int` helpers are used here for `String` and `Int` arguments, respectively). 

The arguments to each of these helpers (and also for `Opt[V].apply()` that they invoke) is the option name, used to retrieve the value later, a `Seq` of flags for command line invocation, an optional default value if the command-line argument isn't used, and a help string for the option.

There are also two helpers for command-line arguments that are strings that contain sequences of elements. We use one of them here, `seqString`, for a classpath-style argument, where the elements will be split into a `Seq[String]`, using `:` and `;` as delimiters; the first argument is a regular expression for the delimiter. There is also a more general `seq[V]` helper, where the string is first split, then parsed into `V` instances. See [Opt.seq[V]](src/main/scala/com/concurrentthought/cla/Opt.scala) for more details.

The first two arguments to the `Args.apply()` method provide help strings. The first shows how to run the application, e.g., `run-main CLASampleMain` as shown, or perhaps `java -cp ... foo.bar.Main`, etc. The string is arbitrary. The second string is an optional description of the program. Finally, a `Seq[Opt[V]]` specifies the actual options supported.

Before discussing the `process` method shown, here is an alternative way to declare `Args`, in `main2`:



```scala
  def main2(argstrings: Array[String]) = {
    import Opt._
    val args = Args("run-main CLASampleMain", "Demonstrates the CLA API.",
      Seq(
        string("input",     Seq("-i", "--in", "--input"),      None,              "Path to input file."),
        string("output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
        int(   "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
        seqString("[:;]")(
               "path",      Seq("-p", "--path"),               None,              "Path elements separated by ':' or ';'.")))

    process(args, argstrings)
  }
```

This is more concise, but harder to follow.

The `process` method uses the `Args`. It first parses the user-specified arguments, returning a new `Args` instance with updated (or default) values for each argument.

```
  protected def process(args: Args, argstrings: Array[String]): Unit = {
    val parsedArgs = args.parse(argstrings)
    ...
```

If errors occurred or help was requested, print the appropriate messages and exit.

```
    ...
    if (parsedArgs.handleErrors()) sys.exit(1)
    if (parsedArgs.handleHelp())   sys.exit(0)
    ...
```

Otherwise, print all the default values or those specified by the user to the command line.

```
    ...
    parsedArgs.printValues()
    ...
```

Finally, extract values and use them. 

```
    ...
    setPathElements(parsedArgs.get[Seq[String]]("path"))
    setLogLevel(parsedArgs.getOrElse("log-level", 0))
  }

  protected def setPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to set!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def setLogLevel(level: Int) =
    println(s"Setting log level to $level")
}
```

The `get[V]` method returns values of the expected type. It uses `asInstanceOf[]` internally, but it should never fail because the parsing process already converted the value to the correct type (and then put them in a `Map[String,Any]`).

 Note that an advantage of `getOrElse[V]` is that its type parameter can be inferred due to the second argument.

 Try running with the help option, `run-main CLASampleMain --help`, then play with the other options. Note the error handling that's done if you omit a value when an option expects one, or an invalid value is given, such as `--log-level foo`.



