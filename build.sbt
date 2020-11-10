name := "oapimerge"

version := "0.1"

scalaVersion := "2.13.3"

scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Ymacro-annotations",
  "-language:postfixOps"
)

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "io.circe" %% "circe-core" % "0.12.3",
  "io.circe" %% "circe-generic" % "0.12.3",
  "io.circe" %% "circe-parser"% "0.12.3",
  "io.circe" %% "circe-yaml" % "0.12.0"
)

dockerRepository := Option(System.getenv("DOCKER_REGISTRY"))
dockerAliases ++= Seq(
    dockerAlias.value.withTag(Option("latest"))
)
dockerBaseImage := "openjdk:11-jre-slim"

enablePlugins(JavaAppPackaging, DockerPlugin)