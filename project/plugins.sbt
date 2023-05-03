resolvers ++= Resolver.sonatypeOssRepos("snapshots")

// Current releases
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.12")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "5.0.0-M3")
