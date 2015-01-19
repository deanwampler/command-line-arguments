import SonatypeKeys._

initialCommands += """
  import com.concurrentthought.cla._
  """

// See https://github.com/xerial/sbt-sonatype
sonatypeSettings

organization := "com.concurrentthought.cla"

profileName := "com.concurrentthought"

pomExtra := {
  <url>https://github.com/deanwampler/command-line-arguments</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>github.com/deanwampler/command-line-arguments</url>
    <connection>scm:git:github.com/deanwampler/command-line-arguments</connection>
    <developerConnection>scm:git:git@github.com:deanwampler/command-line-arguments</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>deanwampler</id>
      <name>Dean Wampler</name>
      <url>http://concurrentthought.com</url>
    </developer>
  </developers>
}
