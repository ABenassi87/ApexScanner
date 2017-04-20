
name := "ApexScanner"

version := "1.0"

scalaVersion := "2.12.2"

//https://blog.threatstack.com/useful-scalac-options-for-better-scala-development-part-1
scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-encoding", "UTF-8",       // yes, this is 2 args
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import",
    "-Ywarn-dead-code",
    "-Ypartial-unification"
)
// disable generation of scala-<version> folders, we do not need cross compilation
crossPaths := false

sources in doc in Compile := List() // do NOT generate Scaladoc, it is a waste of time

// Get rid of scala-{version} folders
sourceDirectories in Compile <<= (sourceDirectories in Compile) { dirs =>
    dirs.filterNot(_.absolutePath.endsWith("-2.11")).filterNot(_.absolutePath.endsWith("-2.12"))
}

libraryDependencies += "org.antlr" %  "antlr4-runtime" % "4.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

