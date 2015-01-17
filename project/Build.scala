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

object BuildSettings {

  val Name = "command-line-arguments"
  val Version = "0.2.1"
  val ScalaVersion  = "2.11.5"
  val CrossScalaVersions = Seq("2.10.4", "2.11.5")

  val allScalacOptions = Vector("-deprecation", "-unchecked", "-feature",
        "-encoding", "utf8",
        "-Xfatal-warnings", "-Xlint", "-Xfuture",
        "-Yno-adapted-args", "-Ywarn-dead-code",
        "-Ywarn-numeric-widen", "-Ywarn-value-discard")
  val scalac211Options = Vector("-Ywarn-infer-any", "-Ywarn-unused-import")

 lazy val buildSettings =
    Defaults.coreDefaultSettings ++ buildInfoSettings ++ releaseSettings ++ Seq (
      name               := Name,
      version            := Version,
      scalaVersion       := ScalaVersion,
      crossScalaVersions := CrossScalaVersions,
      description        := "A library for handling command-line arguments.",

      scalacOptions <<= scalaVersion map { v: String =>
        if (v.startsWith("2.10.")) allScalacOptions 
        else allScalacOptions ++ scalac211Options
      },

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


object Resolvers {
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatype = "Sonatype Release" at "https://oss.sonatype.org/content/repositories/releases"
  val mvnrepository = "MVN Repo" at "http://mvnrepository.com/artifact"

  val allResolvers = Seq(typesafe, sonatype, mvnrepository)
}

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"

  val dependencies = Seq(scalaTest)
}

object CLABuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val claProject = Project(
    id = BuildSettings.Name,
    base = file("."),
    settings = buildSettings ++ Seq(
      shellPrompt := ShellPrompt.prompt(BuildSettings.Version),
      resolvers   := allResolvers,
      exportJars  := true,
      libraryDependencies ++= Dependencies.dependencies))
}



