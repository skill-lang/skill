name := "skill"

version := "0.3"

scalaVersion := "2.11.7"

javacOptions ++= Seq("-encoding", "UTF-8")

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.12" % "test",
	"org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)

exportJars := true

mainClass := Some("de.ust.skill.main.CommandLine")

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/tests")

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
