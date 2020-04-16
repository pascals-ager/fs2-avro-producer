import sbtrelease.ReleaseStateTransformations._

lazy val commonSettings = Seq(
  name := "fs2-avro-producer",
  scalaVersion := "2.13.1",
  organization := "io.pascals.fs2",
  crossScalaVersions := Seq("2.13.1", "2.12.8"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-feature", "-language:higherKinds")
)

lazy val root = (project in file(".")).
  settings(moduleName := "fs2-avro-producer").
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "2.2.2",
      "co.fs2" %% "fs2-io" % "2.2.2",
      "com.github.fd4s" %% "fs2-kafka" % "1.0.0",
      "com.github.fd4s" %% "fs2-kafka-vulcan" % "1.0.0",
      "com.github.fd4s" %% "vulcan-generic" % "1.0.1",
      "io.circe" %% "circe-core" %  "0.13.0",
      "io.circe" %% "circe-generic" %  "0.13.0",
      "io.circe" %% "circe-parser" %  "0.13.0",
      "org.scalatest" %% "scalatest" % "3.1.1" % Test
    )
  )

resolvers += "confluent" at "https://packages.confluent.io/maven/"

publishMavenStyle := true
publishArtifact in Test := false
publishTo := {
  val nexus = "$nexus_repository_here"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "content/repositories/releases")
}
pomIncludeRepository := { _ => false }

releaseTagComment    := s"* Test Releasing ${(version in ThisBuild).value} [skip ci]"

releaseCommitMessage := s"* Test Setting version to ${(version in ThisBuild).value} [skip ci]"

val runUnitTests = ReleaseStep(
  action = Command.process("testOnly * -- -l \"io.pascals.fs2.tags.IntegrationTest\"", _),
  enableCrossBuild = true
)

val runIntegrationTests = ReleaseStep(
  action = Command.process("testOnly * -- -n \"io.pascals.fs2.tags.IntegrationTest\"", _),
  enableCrossBuild = true
)

val publishJar = ReleaseStep(action = Command.process("publish", _), enableCrossBuild = true)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runUnitTests,
  setReleaseVersion,
  publishJar
)