// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.9.0") // Must be the same as BuildSettings.versions.play
// addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.3.1")

// scala.js plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

// Web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.13.0")

// Other
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
// addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
