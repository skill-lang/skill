import de.johoop.jacoco4sbt._
import JacocoPlugin._

name := "skill"

version := "0.2"

scalaVersion := "2.11.2"

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.11" % "test",
	"org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
)

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports")

org.scalastyle.sbt.ScalastylePlugin.Settings

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"

mainClass in oneJar := Some("de.ust.skill.generator.scala.Main")

jacoco.settings
