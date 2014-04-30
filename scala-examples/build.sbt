name := "scala-examples"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.7",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "net.databinder.dispatch" %% "dispatch-lift-json" % "0.11.0",
  "net.liftweb" %% "lift-json" % "2.6-M3"
)