import sbt.{Def, _}
import sbt.Keys._
import com.github.dnvriend.sbt.sam.SAMPluginKeys._

object GlobalBuildSettings extends AutoPlugin {
  override def trigger = allRequirements

  override def requires = plugins.JvmPlugin

  val samVersion = "1.0.31-SNAPSHOT"

  // put these settings at the build level (for all projects)
  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    organization := "com.github.dnvriend",
    samStage := "dev",
    scalaVersion := "2.12.7",
  ) ++ resolverSettings ++ librarySettings

  lazy val resolverSettings = Seq(
    resolvers += Resolver.bintrayRepo("dnvriend", "maven"),
  )

  lazy val librarySettings = Seq(
    libraryDependencies += "com.github.dnvriend" %% "sam-annotations" % samVersion,
    libraryDependencies += "com.github.dnvriend" %% "sam-lambda" % samVersion,
    libraryDependencies += "com.github.dnvriend" %% "sam-serialization" % samVersion,
    libraryDependencies += "com.github.dnvriend" %% "sam-dynamodb-resolver" % samVersion,
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.419",
    libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4",
    libraryDependencies += "org.bouncycastle" % "bcprov-ext-jdk15on" % "1.54",
    libraryDependencies += "com.amazonaws" % "aws-encryption-sdk-java" % "1.3.1",
    libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.9.3",
    libraryDependencies += "io.leonard" %% "play-json-traits" % "1.4.4",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  )
}