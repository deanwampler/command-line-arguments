// Many details adapted from the Cats build: https://github.com/non/cats
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import com.typesafe.sbt.SbtGit.GitKeys._
import com.typesafe.sbt.SbtSite.SiteKeys._
import ReleaseTransformations._
import ScoverageSbtPlugin._

val scala211 = "2.11.12"
val scala212 = "2.12.6"
val scalaDefaultVersion = scala212

lazy val buildSettings = Seq(
  organization       := "com.concurrentthought.cla",
  description        := "A library for handling command-line arguments.",

  scalaVersion       := scalaDefaultVersion,
  crossScalaVersions := Seq(scala212, scala211),

  maxErrors          := 5,
  triggeredMessage   := Watched.clearWhenTriggered,

  scalacOptions in Compile := commonScalacOptions ++ {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) =>
        Seq()
      case Some((2, 11)) =>
        Seq("-Ywarn-infer-any", "-Ywarn-unused-import", "-language:existentials")
      case Some((2, 12)) =>
        Seq("-Ywarn-infer-any", "-Ywarn-unused-import")
      case Some(_) | None =>
        Seq() // should never happen!
    }
  },
  scalacOptions in (Compile, console) := minScalacOptions,
  // scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  // scalacOptions in (Test, console)    ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none",

  libraryDependencies ++= Seq(
    "org.parboiled"  %% "parboiled-scala" % "1.1.8",
    "org.scalatest"  %% "scalatest"       % "3.0.0"  % "test",
    "org.scalacheck" %% "scalacheck"      % "1.13.4" % "test"),

  fork in console  := true
)

lazy val scoverageSettings = Seq(
  coverageMinimum := 60,
  coverageFailOnMinimum := false,
  coverageHighlighting := scalaBinaryVersion.value != "2.10",
  coverageExcludedPackages := "com\\.concurrentthought\\.cla\\.examples\\..*"
)

lazy val minScalacOptions = Seq(
  "-deprecation",
  "-unchecked",
  "-feature",
  "-encoding", "utf8")

lazy val commonScalacOptions = minScalacOptions ++ Seq(
  "-Xfatal-warnings",
  "-Xlint",
  "-Xfuture",
//  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard")

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
