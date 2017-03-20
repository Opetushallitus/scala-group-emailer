import sbt.Keys._
import sbt._

object ScalaGroupEmailerBuild extends Build {
  val Organization = "fi.vm.sade"
  val Name = "scala-group-emailer"
  val Version = "0.3.0-SNAPSHOT"
  val JavaVersion = "1.8"
  val ScalaVersion = "2.11.7"
  val TomcatVersion = "7.0.22"
  val artifactory = "https://artifactory.oph.ware.fi/artifactory"

  if(!System.getProperty("java.version").startsWith(JavaVersion)) {
    throw new IllegalStateException("Wrong java version (required " + JavaVersion + "): " + System.getProperty("java.version"))
  }

  lazy val project = Project (
    Name,
    file("."),
    settings = Defaults.coreDefaultSettings
      ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      javacOptions ++= Seq("-source", JavaVersion, "-target", JavaVersion),
      scalacOptions ++= Seq("-target:jvm-1.7", "-deprecation"),
      resolvers += Resolver.mavenLocal,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "oph-sade-artifactory-releases" at artifactory + "/oph-sade-release-local",
      resolvers += "oph-sade-artifactory-snapshots" at artifactory + "/oph-sade-snapshot-local",
      parallelExecution in Test := false,
      libraryDependencies ++= Seq(
        "fi.vm.sade" %% "scala-utils" % "0.3.1-SNAPSHOT",
        "fi.vm.sade" %% "scala-cas" % "0.4.0-SNAPSHOT",
        "org.http4s" %% "http4s-blaze-client" % "0.13.2a",
        "org.json4s" %% "json4s-core" % "3.5.0",
        "org.json4s" %% "json4s-ext" % "3.5.0",
        "org.json4s" %% "json4s-jackson" % "3.5.0"
      ),
      credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
      publishTo := {
        if (Version.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at artifactory + "/oph-sade-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
        else
          Some("releases" at artifactory + "/oph-sade-release-local")
      }
    )
  ).settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
  lazy val projectRef: ProjectReference = project

}
