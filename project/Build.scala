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
  val Version = "0.2.0"
  val ScalaVersion  = "2.11.4"

  lazy val buildSettings =
    Defaults.coreDefaultSettings ++ buildInfoSettings ++ releaseSettings ++ Seq (
      name          := Name,
      version       := Version,
      scalaVersion  := ScalaVersion,
      description   := "A library for handling command-line arguments.",
      scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8",
        "-Xlint", "-feature", "-Ywarn-infer-any"),
        // This is a nice warning, but it trips up ScalaTest constructs,
        // among others: "-Ywarn-value-discard"

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

  val Prompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (
        currProject, currBranch, BuildSettings.Version
      )
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
      shellPrompt := ShellPrompt.Prompt,
      resolvers   := allResolvers,
      exportJars  := true,
      libraryDependencies ++= Dependencies.dependencies))
}



