import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtGit._
import GitKeys._
import sbtbuildinfo.Plugin._

import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

// Shell prompt which show the current project,
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb".lines_!(devnull).headOption)
      getOrElse "-" stripPrefix "## "
  )

  def prompt(version: String) = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (currProject, currBranch, version)
    }
  }
}

object BuildSettings {

  val Name = "command-line-arguments"
  val Version = "0.4.0"
  val ScalaVersion  = "2.11.7"
  val CrossScalaVersions = Seq("2.10.6", "2.11.7")

  val minScalacOptions = Vector("-deprecation", "-unchecked", "-feature",
    "-encoding", "utf8")
  val commonScalacOptions = minScalacOptions ++ Vector(
    "-Xfatal-warnings", "-Xlint",
    "-Yno-adapted-args", "-Ywarn-dead-code",
    "-Ywarn-numeric-widen", "-Ywarn-value-discard")
  val scalac210Options = commonScalacOptions
  val scalac211Options = commonScalacOptions ++ Vector("-Ywarn-infer-any", "-Ywarn-unused-import")

  lazy val buildSettings =
    Defaults.coreDefaultSettings ++ buildInfoSettings ++ releaseSettings ++ Seq (
      name               := Name,
      version            := Version,
      scalaVersion       := ScalaVersion,
      crossScalaVersions := CrossScalaVersions,
      description        := "A library for handling command-line arguments.",
      maxErrors          := 5,
      triggeredMessage   := Watched.clearWhenTriggered,
      shellPrompt        := ShellPrompt.prompt(Version),

      scalacOptions in Compile <<= scalaVersion map { v: String =>
        if (v.startsWith("2.10.")) scalac210Options
        else scalac211Options
      },
      scalacOptions in (Compile, console) := minScalacOptions,

      fork in console  := true,
      // Don't include the sample program in the `package` jar file.
      // mappings in (Compile, packageBin) ~= { _.filter(!_._1.getName.contains("CLASampleMain")) },

      buildInfoPackage := Name,
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
}


object CLABuild extends Build {
  import Resolvers._
  import BuildSettings._

  val parboiled  = "org.parboiled"  %% "parboiled-scala" % "1.1.7"
  val scalaTest  = "org.scalatest"  %% "scalatest"       % "2.2.4"  % "test"
  val scalaCheck = "org.scalacheck" %% "scalacheck"      % "1.12.5" % "test"

  val dependencies = Seq(parboiled, scalaTest, scalaCheck)

  lazy val cla = project
    .settings(moduleName := BuildSettings.Name)
    .settings(buildSettings)
    .dependsOn(core, examples)

  lazy val core = project.in(file("core"))
    .settings(moduleName := s"${BuildSettings.Name}-core")
    .settings(buildSettings ++ Seq(
      exportJars  := true,
      libraryDependencies ++= dependencies))

  lazy val examples = project.in(file("examples"))
    .settings(moduleName := s"${BuildSettings.Name}-examples")
    .settings(buildSettings)
    .dependsOn(core)
}



