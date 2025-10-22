resolvers ++= Resolver.sonatypeOssRepos("snapshots")

// Current releases
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.9")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.1.1")
