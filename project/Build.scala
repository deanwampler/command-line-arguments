import sbt._
import sbt.Keys._

object BuildSettings {

  val Name = "command-line-arguments"
  val Version = "0.0.1"
  val ScalaVersion = "2.11.4"

  lazy val buildSettings = Defaults.defaultSettings ++ Seq (
    name          := Name,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.concurrentthought",
    description   := "A library for handling command-line arguments.",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )
}


object Resolvers {
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatype = "Sonatype Release" at "https://oss.sonatype.org/content/repositories/releases"
  val mvnrepository = "MVN Repo" at "http://mvnrepository.com/artifact"

  val allResolvers = Seq(typesafe, sonatype, mvnrepository)
}

object Dependencies {

  val scalaTest      = "org.scalatest"  %% "scalatest"    % "2.2.1"  % "test"
  val scalaCheck     = "org.scalacheck" %% "scalacheck"   % "1.12.1" % "test"

  val dependencies = Seq(scalaTest, scalaCheck)
}

object CLABuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val activatorspark = Project(
    id = BuildSettings.Name,
    base = file("."),
    settings = buildSettings ++ Seq(
      shellPrompt := { state => "(%s)> ".format(Project.extract(state).currentProject.id) },
      resolvers := allResolvers,
      exportJars := true,
      libraryDependencies ++= Dependencies.dependencies))
}



