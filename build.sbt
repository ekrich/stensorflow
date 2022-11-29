// stensorflow build
val scala3 = "3.2.1"

val versionsNative = Seq(scala3)

ThisBuild / scalaVersion := scala3
ThisBuild / crossScalaVersions := versionsNative
ThisBuild / versionScheme := Some("early-semver")

inThisBuild(
  List(
    description := "TensorFlow Interface for Scala Native",
    organization := "org.ekrich",
    homepage := Some(url("https://github.com/ekrich/stensorflow")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
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
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-s", "-v"),
  logLevel := Level.Info // Info, Debug
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
  .enablePlugins(ScalaNativePlugin, ScalaNativeJUnitPlugin)
