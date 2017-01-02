name := """dataproducer"""
organization := "org.dw.producer"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += ws
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.1.1"
