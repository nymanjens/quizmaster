// // repository for Typesafe plugins
resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

// Tell the compiler to ignore version mismatches in scala-xml
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.21") // Must be the same as BuildSettings.versions.play
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.2.0") // Replaces sbt-play-scalajs for Play 2.8+

// scala.js plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.14.0")

// Web plugins
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")
// addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.13.0")

// Other
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")
// addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
