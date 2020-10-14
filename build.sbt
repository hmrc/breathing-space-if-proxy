import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "breathing-space-if-proxy"

val silencerVersion = "1.7.0"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.12",
    PlayKeys.playDefaultPort := 9501,
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test,
    // ***************
    // Use the silencer plugin to suppress warnings
    scalacOptions ++= List("-P:silencer:pathFilters=routes", "-Ypartial-unification"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .configs(IntegrationTest extend Test)
  .settings(publishingSettings: _*)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    scoverageSettings,
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true,
    scalafmtOnCompile in IntegrationTest := true
  )

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

unmanagedResourceDirectories in Compile += baseDirectory.value / "public"

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.metrics\\..*",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.views\\..*",
    ".*(Reverse|AuthService|BuildInfo|Routes).*"
  ).mkString(";"),
  coverageMinimum := 96,
  coverageFailOnMinimum := false,
  coverageHighlighting := true,
  parallelExecution in Test := false
)
