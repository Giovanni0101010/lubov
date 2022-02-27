ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "hi"
ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "lubov",
    mainClass := Some("hi.Test")
  )

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.18"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"