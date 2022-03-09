ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "hi"
ThisBuild / scalaVersion := "2.12.15"

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

lazy val root = (project in file("."))
  .settings(
    name := "SmartDDosXaknet",
    mainClass := Some("hi.SmartDDos")
  )

libraryDependencies += "org.scalatestplus" %% "selenium-4-1" % "3.2.10.0"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "4.1.2"
libraryDependencies += "io.github.bonigarcia" % "webdrivermanager" % "5.1.0"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.0"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.0"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.13.0"

/*dependencyOverrides += "io.netty" % "netty-buffer" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-codec-http" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-common" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-transport-classes-epoll" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-transport-classes-kqueue" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-transport-native-unix-common" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-transport" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-codec" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-handler" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-resolver" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-codec-socks" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-handler-proxy" % "4.1.70.Final"
dependencyOverrides += "io.netty" % "netty-handler-proxy" % "4.1.70.Final"*/


libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.8"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.18"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.6"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.10"

libraryDependencies += "org.rogach" %% "scallop" % "4.1.0"