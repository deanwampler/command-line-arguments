// Many details adapted from the Cats build: https://github.com/non/cats
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtSite.SiteKeys._
import ReleaseTransformations._
import ScoverageSbtPlugin._

val scala211 = "2.11.12"
val scala212 = "2.12.9"
val scala213 = "2.13.0"
val scalaDefaultVersion = scala212

// Our set of warts
lazy val myWarts =
  Warts.allBut(
    Wart.Any,                    // false positives
    Wart.ArrayEquals,            // false positives
    Wart.AsInstanceOf,           // casts - but they are hard to avoid in a few places
    Wart.DefaultArguments,       // silly
    Wart.Equals,                 // false positives
    Wart.ImplicitConversion,     // we know what we're doing
    Wart.ImplicitParameter,      // only used for Pos, but evidently can't be suppressed
    Wart.JavaSerializable,       // It appears that Scala 2.13 uses this a lot in type hierarchies
    Wart.MutableDataStructures,  // Will use 'em for efficiency
    Wart.NonUnitStatements,      // false positives in scalatest "intercept" constructs
    Wart.Nothing,                // false positives
    Wart.Null,                   // Java API under the hood; we have to deal with null
    Wart.Overloading,            // We overload "Args.apply"
    Wart.Product,                // false positives
    Wart.PublicInference,        // fails https://github.com/wartremover/wartremover/issues/398
    Wart.Serializable,           // false positives
    Wart.StringPlusAny,          // causes interpolated strings to fail to compile!
    Wart.Throw,                  // there are a few times when we throw exceptions
    Wart.ToString,               // Need to override toString
    Wart.TraversableOps          // Using col.maxBy IS BETTER than requiring a fold
  )

lazy val compilerFlags = Seq(
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 11 => // for 2.11 all we care about is capabilities, not warnings
        Seq(
          "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
          "-language:higherKinds",             // Allow higher-kinded types
          "-language:implicitConversions",     // Allow definition of implicit functions called views
          "-Ypartial-unification"              // Enable partial unification in type constructor inference
        )
      case _ =>
        Seq(
          "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
          "-encoding", "utf-8",                // Specify character encoding used by source files.
          "-explaintypes",                     // Explain type errors in more detail.
          "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
          "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
          "-language:higherKinds",             // Allow higher-kinded types
          "-language:implicitConversions",     // Allow definition of implicit functions called views
          "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
          "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
          "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
          "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
          "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
          "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
          "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
          "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
          "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
          "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
          "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
          "-Xlint:option-implicit",            // Option.apply used implicit view.
          "-Xlint:package-object-classes",     // Class or object defined in package object.
          "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
          "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
          "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
          "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
          // "-Yno-imports",                      // No predef or default imports
          "-Ywarn-dead-code",                  // Warn when dead code is identified.
          "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
          "-Ywarn-numeric-widen",              // Warn when numerics are widened.
          "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
          "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
          "-Ywarn-unused:locals",              // Warn if a local definition is unused.
          "-Ywarn-unused:params",              // Warn if a value parameter is unused.
          "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
          "-Ywarn-unused:privates",            // Warn if a private member is unused.
          "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
        )
    }
  ),
  // flags removed in 2.13
  scalacOptions ++= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n == 12 =>
        Seq(
          "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
          "-Ypartial-unification",             // Enable partial unification in type constructor inference
          "Xfuture"                            // Replaced by -Xsource:2.14, which we don't need yet...
        )
      case _ =>
        Seq.empty
    }
  ),
  scalacOptions in (Test, compile) --= (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 11 =>
        Seq("-Yno-imports")
      case _ =>
        Seq(
          "-Ywarn-unused:privates",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:imports",
          "-Yno-imports"
        )
    }
  ),
  scalacOptions in (Compile, console) --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports"),
  scalacOptions in (Compile, console) ++= Seq("-Ydelambdafy:inline"), // http://fs2.io/faq.html
  scalacOptions in (Compile, doc)     --= Seq("-Xfatal-warnings", "-Ywarn-unused:imports", "-Yno-imports")
)

lazy val buildSettings = Seq(
  organization       := "com.concurrentthought.cla",
  description        := "A library for handling command-line arguments.",
  scalaVersion       := scalaDefaultVersion,
  crossScalaVersions := Seq(scala213, scala212, scala211),
  maxErrors          := 5,
  triggeredMessage   := Watched.clearWhenTriggered,
  fork in console    := true,
  libraryDependencies ++= Seq(
    "org.parboiled"  %% "parboiled-scala" % "1.3.1",
    "org.scalatest"  %% "scalatest"       % "3.0.8"  % "test",
    "org.scalacheck" %% "scalacheck"      % "1.14.0" % "test")) ++
  compilerFlags ++
  Seq(
    // These sbt-header settings can't be set in ThisBuild for some reason
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    headerLicense  := Some(HeaderLicense.Custom(
      """|Copyright (c) 2019 Dean Wampler
         |This software is licensed under the Apache 2.0 License.
         |For more information see LICENSE-2.0.txt or https://www.apache.org/licenses/LICENSE-2.0
         |""".stripMargin
    )),

    // Wartremover in compile and test (not in Console)
    wartremoverErrors in (Compile, compile) := myWarts,
    wartremoverErrors in (Test,    compile) := myWarts,

    scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/deanwampler/command-line-arguments/" + version.value + "/README.md"
    )
  )

lazy val scoverageSettings = Seq(
  coverageMinimum := 60,
  coverageFailOnMinimum := false,
  coverageHighlighting := scalaBinaryVersion.value != "2.10",
  coverageExcludedPackages := "com\\.concurrentthought\\.cla\\.examples\\..*"
)

lazy val sharedPublishSettings = Seq(
  releaseCrossBuild := true,
  releaseTagName := version.value,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean, // disabled to reduce memory usage during release
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges)
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/deanwampler/command-line-arguments")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/deanwampler/command-line-arguments"),
    "scm:git:git@github.com:deanwampler/command-line-arguments.git")),
  // apiURL := Some(url("...")) // TODO
  autoAPIMappings := true,
  pomExtra := (
    <developers>
      <developer>
        <id>deanwampler</id>
        <name>Dean Wampler</name>
        <url>http://concurrentthought.com</url>
      </developer>
    </developers>
  ),
  credentials += Credentials(Path.userHome / ".sonatype" / ".credentials")
) ++ sharedPublishSettings ++ sharedReleaseProcess

lazy val root = project.in(file("."))
  .enablePlugins(ScalaUnidocPlugin, GhpagesPlugin)
  .settings(
    name := "root",
    siteSubdirName in ScalaUnidoc := "latest/api",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    gitRemoteRepo := "git@github.com:deanwampler/command-line-arguments.git",
    skip in publish := true
  )
  .settings(buildSettings)
  .aggregate(core, examples)

lazy val core = project.in(file("core"))
  .settings(name := "command-line-arguments")
  .settings(buildSettings ++ scoverageSettings)
  .settings(publishSettings)

lazy val examples = project.in(file("examples"))
  .settings(name := "command-line-arguments-examples")
  .settings(buildSettings)
  .settings(publishSettings)
  .dependsOn(core)

addCommandAlias("validate", ";scalastyle;test")

initialCommands += """
  import com.concurrentthought.cla._
  import com.concurrentthought.cla.examples._
"""
