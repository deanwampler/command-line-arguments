## Command Line Arguments

Dean Wampler, Ph.D.
[@deanwampler](https://twitter.com/deanwampler)

This is a [Scala](http://scala-lang.org) library for handling command-line arguments. It has few dependencies on other libraries, [Parboiled](https://github.com/sirthias/parboiled/wiki/parboiled-for-Scala), for parsing, and [ScalaTest](http://scalatest.org) and [ScalaCheck](http://scalacheck.org), for testing. So its footprint is small.

## Usage

This library is built for Scala 2.11.12, 2.12.9, and 2.13.0. Artifacts are published to [Sonatype's OSS service](https://oss.sonatype.org/index.html#nexus-search;quick~concurrentthought). You'll need the following settings.

```
resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)
...

scalaVersion := "2.12.9"

libraryDependencies ++= Seq(
  "com.concurrentthought.cla" %% "command-line-arguments"          % "0.6.0"
  "com.concurrentthought.cla" %% "command-line-arguments-examples" % "0.6.0"
)
```

The `examples` can be omitted.

## API

There are several idiomatic ways to setup and use the API. The simplest approach parses a multi-line string to specify the command-line arguments [com.concurrentthought.cla.Args](src/main/scala/com/concurrentthought/cla/Args.scala) using what is essentially an embedded DSL (domain specific language).

The [com.concurrentthought.cla.CLASampleMain](examples/src/main/scala/com/concurrentthought/cla/CLASampleMain.scala) in the `examples` subproject shows two different idiomatic ways to set up and use the API. For example:

```scala
import com.concurrentthought.cla._

object CLASampleMain {

  def main(argstrings: Array[String]) = {
    val initialArgs: Args = """
      |run-main CLASampleMain [options]
      |Demonstrates the CLA API.
      |   -i | --in  | --input      string              Path to input file.
      |  [-o | --out | --output     string=/dev/null]   Path to output file.
      |  [-l | --log | --log-level  int=3]              Log level to use.
      |  [-p | --path               path]               Path elements separated by ':' (*nix) or ';' (Windows).
      |  [--things                  seq([-|])]          String elements separated by '-' or '|'.
      |  [-q | --quiet              flag]               Suppress some verbose output.
      |                             others              Other arguments.
      |Note that --input and "others" are required.
      |""".stripMargin.toArgs

    // Process the input arguments. If help requested or an error occurs,
    // a message is written to stdout and the program exits with an error code.
    // Default arguments for `process` aren't shown. See also Args#parse() for
    // more flexible handling.
    val finalArgs: Args = initialArgs.process(argstrings)

    // If here, successfully parsed the args and none where "--help" or "-h".
    showResults(finalArgs)
  }
  ...
```

To run this example using `sbt`:

```
sbt:command-line-arguments> project examples
sbt:command-line-arguments-examples> run -h
[info] Running com.concurrentthought.cla.examples.CLASampleMain -h
Usage: run-main CLASampleMain [options] [options]
Demonstrates the CLA API.
Where the supported options are the following:

  [-h | --h | --help]                      Show this help message.
   -i | --in | --input  input              Path to input file.
  [-o | --out | --output  output]          Path to output file.
                                           (default: /dev/null)
  [-l | --log | --log-level  log-level]    Log level to use.
                                           (default: 3)
  [-p | --path  path]                      Path elements separated by ':' (*nix) or ';' (Windows).
  [--things  things]                       String elements separated by '-' or '|'.
  [-q | --quiet]                           Suppress some verbose output.
   others                                  Other arguments.

You can also use --foo=bar syntax. Arguments shown in [...] are optional. All others are required.
Note that --input and "others" are required.


Exception: sbt.TrapExitSecurityException thrown from the UncaughtExceptionHandler in thread "run-main-2"
[success] Total time: 0 s, completed Aug 22, 2019 3:23:16 PM
sbt:command-line-arguments-examples>
```
The [Scaladocs comments](src/main/scala/com/concurrentthought/cla/package.scala) for the [cla package](src/main/scala/com/concurrentthought/cla/package.scala) explain the format and its limitations, but hopefully most of the format is reasonable intuitive from the example.

The first and last lines in the string that *don't* have leading whitespace are interpreted as lines to show as part of the corresponding help message. It's a good idea to use the first line to show an example of how to invoke the program.

Next come the command-line options, one per line. Each must start with whitespace, followed by zero or more flags separated by `|`. There can be at most one option that has no flags. It is used to provide a help message for how command-line tokens that aren't associated with flags will be interpreted. (Note that the library will still handle these tokens whether or not you specify a line like this.)

To indicate that an option can be omitted by the user (i.e., it's truly _optional_), the flags and name must be wrapped in `[...]`. Otherwise, the user must specify the option explicitly on the command line. However, if a default value is specified (discussed next), it makes an option _optional_ anyway. The purpose of the optional feature is to indicate to the user which arguments are required and to automatically report missing arguments as errors.

In this example, all are optional except for the `--input` and `others` arguments.

The center "column" specifies the type of the option. All but the `flag` and `~flag` types accept an optional default value, which is indicated with an equals `=` sign. The following "types" are supported:

|   String | Interpretation  | Corresponding Helper Method    |
| -------: | :-------------- | :----------------------------- |
|  `flag`  | `Boolean` value | [Opt.flag](src/main/scala/com/concurrentthought/cla/Opt.scala) |
| `~flag`  | `Boolean` value | [Opt.flag](src/main/scala/com/concurrentthought/cla/Opt.scala) |
| `string` | `String` value  | [Opt.string](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|   `byte` |   `Byte` value  | [Opt.byte](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|   `char` |   `Char` value  | [Opt.char](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|    `int` |    `Int` value  | [Opt.int](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|   `long` |   `Long` value  | [Opt.long](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|  `float` |  `Float` value  | [Opt.float](src/main/scala/com/concurrentthought/cla/Opt.scala) |
| `double` | `Double` value  | [Opt.double](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|   `seq`  | `Seq[String]` [1] | [Opt.seqString](src/main/scala/com/concurrentthought/cla/Opt.scala) |
|  `path`  | "path-like" `Seq[String]` [1] | [Opt.path](src/main/scala/com/concurrentthought/cla/Opt.scala) |
| *other*  | Only allowed for the single, no-flags case | [Args.remainingOpt](src/main/scala/com/concurrentthought/cla/Args.scala) |


1: Both `path` and `seq` split an argument using the delimiter regular expression. For `path`, this is the platform-specific path separator, given by `sys.props.getOrElse("path.separator", ":")`. It is designed for class paths, etc. For `seq`, you must provide the delimiter regular expression using a suffix of the form `(delimRE)`. In the example above, the regex is `[-|]` (split on either `-` or `|`).

Both `flag` and `~flag` represent `Boolean` flags where no default value can be supplied (e.g., `--help`). The value corresponding to a `flag` defaults to `false` if the user doesn't invoke the flag on the command line, `~flag` ("tilde" or "not" flag) defaults to `true`.

So, when an option expects something other than a `String`, the token given on the command line (or as a default value) will be parsed into the correct type, with error handling captured in the [Args.failures](src/main/scala/com/concurrentthought/cla/Args.scala) field.

Finally, the rest of the text on a line is the help message for the option.

Before discussing the `process` method shown, let's see two alternative, programmatic ways to declare [Args](src/main/scala/com/concurrentthought/cla/Args.scala) using the API:

```scala
  ...
  def main2(argstrings: Array[String]) = {
    val input  = Opt.string(
      name     = "input",
      flags    = Seq("-i", "--in", "--input"),
      help     = "Path to input file.",
      requiredFlag = true)
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
    val path = Opt.path(
      name     = "path",
      flags    = Seq("-p", "--path"))
    val things = Opt.seqString(delimsRE = "[-|]")(
      name     = "things",
      flags    = Seq("--things"),
      help     = "String elements separated by '-' or '|'.")
    val others = Args.makeRemainingOpt(
      name     = "others",
      help     = "Other arguments",
      requiredFlag = true)

    val initialArgs = Args(
      "run-main CLASampleMain [options]",
      "Demonstrates the CLA API.",
      """Note that --input and "others" are required.""",
      Seq(input, output, logLevel, path, things, Args.quietFlag, others))

    val finalArgs: Args = initialArgs.process(argstrings)
    showResults(finalArgs)
  }
  ...
}
```

Each option is defined using a [com.concurrentthought.cla.Opt](src/main/scala/com/concurrentthought/cla/Opt.scala) value. In this case, there are helper methods in the `Opt` companion object for constructing options where the values are strings or numbers. The `string` and `int` helpers are used here for `String` and `Int` arguments, respectively).

The arguments to each of these helpers (and also for `Opt[V].apply()` that they invoke) is the option name, used to retrieve the value later, a `Seq` of flags for command line invocation, an optional default value if the command-line argument isn't used (defaults to `None`), a help string (defaults to ""), and a boolean flag indicating whether or not the "option" is required (defaults to `false`, which is sort of the opposite behavior of the string DSL discussed previously).

There are also two helpers for command-line arguments that are strings that contain sequences of elements. We use one of them here, `seqString`, for a classpath-style argument, where the elements will be split into a `Seq[String]`, using `:` and `;` as delimiters; the first argument is a regular expression for the delimiter. If you want to support a path-like option, e.g., a `CLASSPATH`, there is another, even more specific helper, `Opt.path`, that handles the platform-specific value for the path-element separator.

There is also a more general `seq[V]` helper, where the string is first split, then parsed into `V` instances. See [Opt.seq[V]](src/main/scala/com/concurrentthought/cla/Opt.scala) for more details.

The first two arguments to the `Args.apply()` method provide help strings. The first shows how to run the application, e.g., `run-main CLASampleMain` as shown, or perhaps `java -cp ... foo.bar.Main`, etc. The string is arbitrary. The second string is an optional description of the program. Finally, a `Seq[Opt[V]]` specifies the actual options supported. Note that we didn't define a `Flag` for quiet, as in the first example, instead we used a built-in flag `Args.quietFlag`.

Here is a slightly more concise way to write the content in `main2`:

```scala
  ...
  def main3(argstrings: Array[String]) = {
    import Opt._
    import Args._
    val initialArgs = Args(
      "run-main CLASampleMain [options]",
      "Demonstrates the CLA API.",
      """Note that --input and "others" are required.""",
      Seq(
        string("input",     Seq("-i", "--in", "--input"),      None,              "Path to input file."),
        string("output",    Seq("-o", "--out", "--output"),    Some("/dev/null"), "Path to output file."),
        int(   "log-level", Seq("-l", "--log", "--log-level"), Some(3),           "Log level to use."),
        path(  "path",      Seq("-p", "--path"),               None),
        seqString("[:;]")(
               "things",    Seq("--things"),                   None,              "String elements separated by '-' or '|'."),
        Args.quietFlag,
        makeRemainingOpt(
               "others",                                                          "Other arguments", true)))

    val finalArgs: Args = initialArgs.process(argstrings)
    showResults(finalArgs)
  }
  ...
```

This is more concise, but perhaps harder to follow.

The [Args#process](src/main/scala/com/concurrentthought/cla/Args.scala) first calls `Args#parse` on the user-specified arguments, which returns a new `Args` instance with updated values for each argument. However, if an error occurs or help is requested, `process` automatically prints a message and exits. This behavior is configurable by overriding default arguments. See also `Args#parse()` for more flexible handling.

You'll almost always want to include logic like this in your code that uses this library.

If `--quiet` wasn't specified, then you might print information about the argument values. We demonstrate this in the `CLASampleMain` program. where the `showResults` method prints all the options and the current values for them, either the defaults or the user-specified values.

```
  protected def showResults(parsedArgs: Args): Unit = {
    if (parsedArgs.getOrElse("quiet", false)) {
      println("(... I'm being very quiet...)")
    } else {
      // Print all the default values or those specified by the user.
      parsedArgs.printValues()

      // Print all the values including repeats.
      parsedArgs.printAllValues()

      // Repeat the "other" arguments (not associated with flags).
      println("\nYou gave the following \"other\" arguments: " +
        parsedArgs.remaining.mkString(", "))
      ...
```

What's the difference between `printValues` and `printAllValues`. They address the case where the user should be able to repeat some options, for example, multiple sources of input, while other examples should only be used once. To simplify handling, the API remembers all occurrences of an option on the command line. The method `printAllValues` and the corresponding `getAll` and `getAllOrElse` methods print or return all occurrences seen, respectively. So, if you want an option to be repeatable, retrieve the results with `getAll` or `getAllOrElse`. Otherwise, use `get` and `getOrElse`, which return the *last* occurrence of an option (or the default, if any). This supports the common practice in POSIX systems of allowing subsequent option occurrences to override previous occurrences on a command line.

Finally, we extract some other values and "use" them.

```
    ...
      showPathElements(parsedArgs.get[Seq[String]]("path"))
      showLogLevel(parsedArgs.getOrElse("log-level", 0))
      println
    }
  }

  protected def showPathElements(path: Option[Seq[String]]) = path match {
    case None => println("No path elements to show!")
    case Some(seq) => println(s"Setting path elements to $seq")
  }

  protected def showLogLevel(level: Int) =
    println(s"New log level: $level")
}
```

The `get[V]` method returns values of the expected type. It uses `asInstanceOf[]` internally, but it should never fail because the parsing process already converted the value to the correct type (and then put it in a `Map[String,Any]` used by `get[V]`).

Note that an advantage of `getOrElse[V]` is that its type parameter can be inferred due to the second argument.

Try running the following examples within SBT, where `run` and `run-main com.concurrentthought.cla.CLASampleMain` do the same thing since there is only one object with a `main` method. (This time I'll just use `>` for the SBT prompt.):

```
> project examples
> run-main com.concurrentthought.cla.CLASampleMain -h
> run -h
> run --help
> run -i /in -o /out -l 4 -p a:b --things x-y|z foo bar baz
> run -i /in -o /out -l 4 -p a:b --things x-y|z foo bar baz --quiet
> run --in /in --out=/out -l=4 --path "a:b" --things=x-y|z foo bar baz
```

The last example mixes `argflag value` and `argflag=value` syntax, which are both supported.

Try a few runs with unknown flags and other errors. Note the error handling that's done, such as when you omit a value expected by a flag, or you provide an invalid value, such as `--log-level foo`.
