ThisBuild / scalaVersion := "2.13.12"
ThisBuild / name := (server / name).value
name := (ThisBuild / name).value

val V = new {
  val betterMonadicFor = "0.3.1"
  val cats = "2.6.1"
  val catsEffect = "3.3.14"
  val circe = "0.14.3"
  val fs2 = "3.3.0"
  val fs2Dom = "0.1.0-M1"
  val http4s = "0.23.30"
  val http4sDom = "0.2.0"
  val http4sSpa = "0.6.1"
  val logbackClassic = "1.5.6"
  val scalajsDom = "2.1.0"
  val scalajsReact = "2.0.0"
}

lazy val commonSettings: Seq[Setting[_]] = Seq(
  version := {
    val Tag = "refs/tags/v?([0-9]+(?:\\.[0-9]+)+(?:[+-].*)?)".r
    sys.env.get("CI_VERSION").collect { case Tag(tag) => tag }
      .getOrElse("0.0.1-SNAPSHOT")
  },
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor),
  assembly / assemblyJarName := s"${name.value}-${version.value}.sh.bat",
  assembly / assemblyOption := (assembly / assemblyOption).value
    .withPrependShellScript(Some(AssemblyPlugin.defaultUniversalScript(shebang = false))),
  assembly / assemblyMergeStrategy := {
    case PathList(paths@_*) if paths.last == "module-info.class" => MergeStrategy.discard
    case PathList("libws281x.so") => MergeStrategy.last
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  },
)

lazy val root = project.in(file("."))
  .settings(
    publishArtifact := false
  )
  .aggregate(server)

lazy val shared = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-io" % V.fs2,
      "io.circe" %%% "circe-core" % V.circe,
      "io.circe" %%% "circe-generic" % V.circe,
      "io.circe" %%% "circe-parser" % V.circe,
      "org.http4s" %%% "http4s-circe" % V.http4s,
      "org.http4s" %%% "http4s-client" % V.http4s,
      "org.typelevel" %%% "cats-core" % V.cats,
      "org.typelevel" %%% "cats-effect" % V.catsEffect,
    )
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val frontend = project
  .enablePlugins(ScalaJSWebjarPlugin)
  .dependsOn(sharedJs)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.armanbilge" %%% "fs2-dom" % V.fs2Dom,
      "com.github.japgolly.scalajs-react" %%% "core-bundle-cats_effect" % V.scalajsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % V.scalajsReact,
      "org.scala-js" %%% "scalajs-dom" % V.scalajsDom,
      "org.http4s" %%% "http4s-dom" % V.http4sDom
    ),

    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    scalaJSUseMainModuleInitializer := true,
  )

lazy val frontendWebjar = frontend.webjar
  .settings(
    webjarAssetReferenceType := Some("http4s"),
    libraryDependencies += "org.http4s" %% "http4s-server" % V.http4s,
  )

lazy val server = project
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(sharedJvm, frontendWebjar)
  .settings(commonSettings)
  .settings(
    name := "pixelmapper",

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % V.logbackClassic,
      "com.github.mbelling" % "rpi-ws281x-java" % "2.0.0",
      "org.http4s" %% "http4s-circe" % V.http4s,
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "org.http4s" %% "http4s-ember-server" % V.http4s,
      "de.lolhens" %% "http4s-spa" % V.http4sSpa
    )
  )
