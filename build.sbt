name := "skill"

version := "0.9"

scalaVersion := "2.12.4"

javacOptions ++= Seq("-encoding", "UTF-8")

compileOrder := CompileOrder.JavaThenScala

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.12" % "test",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

exportJars := true

mainClass := Some("de.ust.skill.main.CommandLine")

(testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/tests")

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

libraryDependencies += "org.json" % "json" % "20160810"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "de.ust.skill"
  )

assemblyJarName in assembly := "skill.jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}
test in assembly := {}
