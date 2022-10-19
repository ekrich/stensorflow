addCommandAlias("run", "stensorflow/run")

val scala211 = "2.11.12"
val scala212 = "2.12.16"
val scala213 = "2.13.10"
val scala300 = "3.1.0"

val versionsNative = Seq(scala211, scala212, scala213)

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := versionsNative
ThisBuild / versionScheme := Some("early-semver")

inThisBuild(
  List(
    description := "TensorFlow Interface for Scala Native",
    organization := "org.ekrich",
    homepage := Some(url("https://github.com/ekrich/stensorflow")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        id = "ekrich",
        name = "Eric K Richardson",
        email = "ekrichardson@gmail.com",
        url = url("http://github.ekrich.org/")
      )
    )
  )
)

lazy val commonSettings = Seq(
  addCompilerPlugin(
    "org.scala-native" % "junit-plugin" % nativeVersion cross CrossVersion.full),
  libraryDependencies += "org.scala-native" %%% "junit-runtime" % nativeVersion,
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-s", "-v"),
  logLevel := Level.Info, // Info, Debug
  nativeLinkStubs := true
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "stensorflow-root",
    crossScalaVersions := Nil,
    publish / skip := true,
    doc / aggregate := false,
    doc := (stensorflow / Compile / doc).value,
    packageDoc / aggregate := false,
    packageDoc := (stensorflow / Compile / packageDoc).value
  )
  .aggregate(stensorflow)

lazy val stensorflow = project
  .in(file("stensorflow"))
  .settings(
    crossScalaVersions := versionsNative,
    commonSettings
  )
  .enablePlugins(ScalaNativePlugin)
