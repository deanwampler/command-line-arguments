// Many details adapted from the Cats build: https://github.com/non/cats
import com.typesafe.sbt.pgp.PgpKeys.publishSigned
import com.typesafe.sbt.SbtSite.SiteKeys._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

import com.typesafe.sbt.SbtGit._
import GitKeys._

lazy val scalaVersionString = "2.11.7"

lazy val buildSettings = Seq(
  organization       := "com.concurrentthought.cla",
  name               := "command-line-arguments",
  description        := "A library for handling command-line arguments.",
  version            := "0.4.0",

  scalaVersion       := scalaVersionString,
  crossScalaVersions := Seq("2.10.6", "2.11.7"),

  maxErrors          := 5,
  triggeredMessage   := Watched.clearWhenTriggered,

  scalacOptions in Compile            := commonScalacOptions,
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
) ++ extraWarnings


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

lazy val extraWarnings = Seq(
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) =>
        Seq()
      case Some((2, n)) if n >= 11 =>
        Seq("-Ywarn-infer-any", "-Ywarn-unused-import")
    }
  },
  scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)},
  scalacOptions in (Test, console) <<= (scalacOptions in (Compile, console))
)

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

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/deanwampler/command-line-arguments")),
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/deanwampler/command-line-arguments"), 
    "scm:git:git@github.com:deanwampler/command-line-arguments.git")),
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
  ),
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
) ++ sharedPublishSettings ++ sharedReleaseProcess 

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)


lazy val cla = project.in(file("."))
  .settings(moduleName := "root")
  .settings(buildSettings)
  .settings(noPublishSettings)
  .aggregate(core, examples)
  .dependsOn(core, examples)

lazy val core = project.in(file("core"))
  .settings(moduleName := "command-line-arguments")
  .settings(buildSettings)
  .settings(publishSettings)

lazy val examples = project.in(file("examples"))
  .settings(moduleName := "command-line-arguments-examples")
  .settings(buildSettings)
  .settings(publishSettings)
  .dependsOn(core)


initialCommands += """
  import com.concurrentthought.cla._
  """
