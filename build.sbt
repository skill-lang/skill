name := "skill"

version := "0.3"

scalaVersion := "2.11.4"

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.11" % "test",
	"org.scalatest" % "scalatest_2.11" % "2.2.0" % "test"
)

exportJars := true

mainClass := Some("de.ust.skill.main.CommandLine")

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/tests")

org.scalastyle.sbt.ScalastylePlugin.Settings

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
