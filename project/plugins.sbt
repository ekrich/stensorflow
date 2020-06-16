resolvers += Resolver.sonatypeRepo("snapshots")

// Current release 0.4.0-M2
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.0-SNAPSHOT")
addSbtPlugin("com.geirsson"     % "sbt-ci-release"   % "1.5.3")
