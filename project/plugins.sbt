resolvers += Resolver.sonatypeRepo("snapshots")

// Current releases
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.6")
addSbtPlugin("com.github.sbt"   % "sbt-ci-release"   % "1.5.10")
addSbtPlugin("com.dwijnand"     % "sbt-dynver"       % "5.0.0-M3")
