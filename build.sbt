lazy val prevVersion = "0.1.0"
lazy val nextVersion = "0.1.0"

lazy val scala211 = "2.11.12"

scalaVersion := scala211

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
    ),
    version := dynverGitDescribeOutput.value.mkVersion(versionFmt, ""),
    dynver := sbtdynver.DynVer
      .getGitDescribeOutput(new java.util.Date)
      .mkVersion(versionFmt, "")
  ))

// stable snapshot is not great for publish local
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val tag = out.ref.dropV.value
  if (out.isCleanAfterTag) tag
  else nextVersion + "-SNAPSHOT"
}

lazy val commonSettings = Seq(
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.6.10-SNAPSHOT" % Test,
  testFrameworks += new TestFramework("utest.runner.Framework"),
  scalaVersion := scala211,
  logLevel := Level.Info, // Info, Debug
  nativeLinkStubs := true
//  nativeMode := "release"
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
    commonSettings,
    nativeCodeInclude := true,
    //updateOptions := updateOptions.value.withLatestSnapshots(false),
  )
  .enablePlugins(ScalaNativePlugin)
