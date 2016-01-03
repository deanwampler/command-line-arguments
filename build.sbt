// Many details adapted from the Cats build: https://github.com/non/cats
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
// import sbt._
// import sbt.Keys._

import com.typesafe.sbt.SbtGit._
import GitKeys._
// import SiteKeys._
// import sbtbuildinfo.Plugin._

// import sbtrelease._
// import sbtrelease.ReleasePlugin._
// import sbtrelease.ReleasePlugin.ReleaseKeys._
// import sbtrelease.ReleaseStateTransformations._
// import sbtrelease.Utilities._

// import SonatypeKeys._

// See https://github.com/xerial/sbt-sonatype
// sonatypeSettings

lazy val buildSettings = Seq(
  organization       := "com.concurrentthought.cla",
  name               := "command-line-arguments",
  description        := "A library for handling command-line arguments.",
  version            := "0.4.0",

  scalaVersion       := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7"),

  maxErrors          := 5,
  triggeredMessage   := Watched.clearWhenTriggered,

  scalacOptions in Compile <<= scalaVersion map { v: String =>
    if (v.startsWith("2.10.")) scalac210Options
    else scalac211Options
  },
  scalacOptions in (Compile, console) := minScalacOptions,

  fork in console  := true,

  libraryDependencies ++= Seq(
    "org.parboiled"  %% "parboiled-scala" % "1.1.7",
    "org.scalatest"  %% "scalatest"       % "2.2.4"  % "test",
    "org.scalacheck" %% "scalacheck"      % "1.12.5" % "test"
    ),

    buildInfoPackage := name.value,
    buildInfoKeys := Seq[BuildInfoKey](version, scalaVersion),
    buildInfoKeys ++= Seq[BuildInfoKey](
      version,
      scalaVersion,
      gitHeadCommit,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    )
)


lazy val minScalacOptions = Vector(
  "-deprecation", 
  "-unchecked", 
  "-feature",
  "-encoding", "utf8")

lazy val commonScalacOptions = minScalacOptions ++ Vector(
  "-Xfatal-warnings",
  "-Xlint",
  "-Xfuture",
  "-Yinline-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard")

lazy val scalac210Options = commonScalacOptions
lazy val scalac211Options = commonScalacOptions ++ Vector(
  "-Ywarn-infer-any",
  "-Ywarn-unused-import")


lazy val sharedPublishSettings = Seq(
  releaseCrossBuild := true,
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
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges)
)

// `credentials` is a `Credentials` instance defined in my private
// ~/.sbt/0.13/sonatype.sbt file. E.g.,
//   credentials += Credentials("Sonatype Nexus Repository Manager",
//     "oss.sonatype.org", name, password)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/deanwampler/command-line-arguments")),
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(ScmInfo(url("https://github.com/deanwampler/command-line-arguments"), "scm:git:git@github.com:deanwampler/command-line-arguments.git")),
  autoAPIMappings := true,
  apiURL := Some(url("https://non.github.io/deanwampler/command-line-arguments/api/")),
  pomExtra := (
    <developers>
      <developer>
        <id>deanwampler</id>
        <name>Dean Wampler</name>
        <url>http://concurrentthought.com</url>
      </developer>
    </developers>
  )
) ++ Seq(credentials) ++ sharedPublishSettings ++ sharedReleaseProcess 

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)


lazy val cla = project.in(file("."))
  .settings(moduleName := "root")
  .settings(buildSettings:_*)
  .settings(noPublishSettings)
  .aggregate(core, examples)
  .dependsOn(core, examples)

lazy val core = project.in(file("core"))
  .settings(moduleName := "command-line-arguments")
  .settings(buildSettings:_*)

lazy val examples = project.in(file("examples"))
  .settings(moduleName := "command-line-arguments-examples")
  .settings(buildSettings:_*)
  .dependsOn(core)


initialCommands += """
  import com.concurrentthought.cla._
  """
