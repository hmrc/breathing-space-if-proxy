import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "breathing-space-if-proxy"

val silencerVersion = "1.7.1"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(
    majorVersion := 1,
    scalaVersion := "2.12.12",
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test,
    PlayKeys.playDefaultPort := 9501,
    TwirlKeys.templateImports := Seq(),
    routesImport += "uk.gov.hmrc.breathingspaceifproxy.config.Binders._",
    // ***************
    // Use the silencer plugin to suppress warnings
    scalacOptions ++= List("-P:silencer:pathFilters=routes", "-Ypartial-unification", "-Xfatal-warnings", "-feature"),
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
    ThisBuild / scalafmtOnCompile := true
  )

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Compile / unmanagedResourceDirectories += baseDirectory.value / "public"

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.connector\\.test\\..*",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.controller\\.test\\..*",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.metrics\\..*",
    "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.views\\..*",
    ".*(Reverse|AuthService|BuildInfo|Routes).*"
  ).mkString(";"),
  coverageMinimumStmtTotal := 96,
  coverageFailOnMinimum := false,
  coverageHighlighting := true,
  Test / parallelExecution := false
)
