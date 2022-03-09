ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "hi"
ThisBuild / scalaVersion := "2.12.15"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "SmartDDos",
    mainClass := Some("hi.SmartDDos")
  )

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.18"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

libraryDependencies += "org.scalatestplus" %% "scalatestplus-selenium" % "1.0.0-M2"
libraryDependencies += "org.scalatestplus.play" % "scalatestplus-play_2.12" % "5.1.0"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chromium-driver" % "4.1.2"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "4.1.2"
libraryDependencies += "org.scalatestplus" %% "selenium-3-141" % "3.3.0.0-SNAP3"