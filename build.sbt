import sbt.Keys._
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings


val appName = "breathing-space-if-proxy"

scalaVersion := "2.13.8"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(
    majorVersion := 1,
    scalaVersion := "2.13.8",
    libraryDependencies ++= Dependencies.compile ++ Dependencies.test,
    PlayKeys.playDefaultPort := 9501,
    TwirlKeys.templateImports := Seq(),
    routesImport += "uk.gov.hmrc.breathingspaceifproxy.config.Binders._",
  )
  .configs(IntegrationTest extend Test)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    scoverageSettings,
    ThisBuild / scalafmtOnCompile := true,
    scalacOptions ++= Seq(
      "-Werror",
      "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s"
    )
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
