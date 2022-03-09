ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "hi"
ThisBuild / scalaVersion := "2.12.15"

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "SmartDDosXaknet",
    mainClass := Some("hi.SmartDDos")
  )

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "3.141.59"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59"
libraryDependencies += "io.github.bonigarcia" % "webdrivermanager" % "4.0.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.15"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.15"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

//libraryDependencies += "org.rogach" %% "scallop" % "4.1.0"