import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "hi"
ThisBuild / scalaVersion := "2.12.15"

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "Sync",
    mainClass := Some("hi.SlowSync")
  )

libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59",
  "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59",
  "io.github.bonigarcia" % "webdrivermanager" % "4.0.0",
  "com.typesafe.akka" %% "akka-actor" % "2.6.18",
  "com.typesafe.akka" %% "akka-http" % "10.2.9",
  "com.typesafe.akka" %% "akka-stream" % "2.6.18",
  "io.spray" %% "spray-json" % "1.3.6",
  "com.github.tototoshi" %% "scala-csv" % "1.3.10",
  "log4j" % "log4j" % "1.2.14"
)