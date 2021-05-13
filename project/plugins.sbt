resolvers += Resolver.sonatypeRepo("snapshots")

// Current release 0.4.0
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0")
addSbtPlugin("com.geirsson"     % "sbt-ci-release"   % "1.5.7")
