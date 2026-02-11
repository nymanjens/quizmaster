import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

/**
  * Application settings. Configure the build for your application here.
  * You normally don't have to touch the actual build definition after this.
  */
object BuildSettings {

  /** The name of your application */
  val name = "quizmaster"

  /** The version of your application */
  val version = "1.0"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-encoding",
    "UTF-8",
    "-feature", // Emit warning and location for usages of features that should be imported explicitly
    "-unchecked", // Enable additional warnings where generated code depends on assumptions
    "-Xfuture", // Warn of changes to future major Scala version
    "-Xlint:-unused,_",
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ywarn-dead-code",
    "-Ywarn-unused:-imports"
    // "-Xfatal-warnings", // Make warnings behave like errors
    //"-deprecation", // Emit warning and location for usages of deprecated APIs
    // "-Ywarn-value-discard" // Warning for value discard bugs (see
    // http://underscore.io/blog/posts/2016/11/24/value-discard.html)
    // Disabled because some cases (e.g. Guice modules) actually
    // benefit from this feature.
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.13.12" // Must be the same as in .travis.yml
    val play = "2.8.21" // Must be the same as the Play sbt-plugin in plugins.sbt

    val uTest = "0.8.1"
    val scalajsReact = "2.1.1"
  }

  /**
    * These dependencies are shared between JS and JVM projects
    * the special %%% function selects the correct version for each project
    */
  val sharedDependencies = Def.setting(
    Seq(
      "org.scala-lang.modules" %% "scala-async" % "1.0.1",
      "com.lihaoyi" %%% "autowire" % "0.3.3",
      "io.suzaku" %%% "boopickle" % "1.4.0"
    ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(
    Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.2.0",
      "com.lihaoyi" %% "utest" % versions.uTest % Test,
      "com.typesafe.play" %% "play-jdbc" % versions.play,
      "com.typesafe.play" %% "play-cache" % versions.play,
      "com.typesafe.play" %% "play-ws" % versions.play,
      "com.typesafe.play" %% "play-specs2" % versions.play % Test,
      "com.lihaoyi" %% "pprint" % "0.9.0" % Test,
      "com.google.truth" % "truth" % "1.1.5" % Test,
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
      "commons-lang" % "commons-lang" % "2.6",
      "mysql" % "mysql-connector-java" % "8.0.33",
      "com.h2database" % "h2" % "2.2.224",
      "org.xerial" % "sqlite-jdbc" % "3.42.0.0",
      "com.google.code.findbugs" % "jsr305" % "3.0.2",
      "org" % "jaudiotagger" % "2.0.3",
      "com.google.guava" % "guava" % "32.1.2-jre",
      "com.googlecode.json-simple" % "json-simple" % "1.1.1",
      "org.yaml" % "snakeyaml" % "2.2",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "net.jcip" % "jcip-annotations" % "1.0"
    ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(
    Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % versions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % versions.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "test" % versions.scalajsReact % Test,
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "org.scala-js" %%% "scalajs-java-time" % "1.0.0",
      "com.lihaoyi" %%% "utest" % versions.uTest % Test
    ))

  def npmDependencies(projectRootDirectory: File): Seq[(String, String)] = Seq(
    // For assets only
    "jquery" -> "1.11.1",
    "bootstrap" -> "3.3.6",
    "metismenu" -> "1.1.3",
    "font-awesome" -> "4.6.3",
    "startbootstrap-sb-admin-2" -> "1.0.7",
    // Used in ScalaJS code
    "lokijs" -> "1.4.2",
    "react" -> "16.3.1",
    "react-dom" -> "16.3.1",
    "react-beautiful-dnd" -> "10.0.3",
    "escape-html" -> "1.0.3",
    "mousetrap" -> "^1.6.2",
    "recharts" -> "1.8.5",
    "global-mousetrap" -> s"file:${projectRootDirectory / "app/js/shared/src/main/npm-packages/global-mousetrap"}"
  )
}
