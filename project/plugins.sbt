resolvers += Resolver.sonatypeRepo("snapshots")

// Current releases
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.2")
addSbtPlugin("com.github.sbt"   % "sbt-ci-release"   % "1.5.10")
