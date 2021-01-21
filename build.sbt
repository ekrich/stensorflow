addCommandAlias("run", "stensorflow/run")

val scala211 = "2.11.12"

ThisBuild / scalaVersion := scala211

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
    commonSettings
  )
  .enablePlugins(ScalaNativePlugin)
