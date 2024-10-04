/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.*
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.*

val appName = "breathing-space-if-proxy"
val scala2_13 = "2.13.12"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := scala2_13
ThisBuild / scalafmtOnCompile := true

lazy val plugins: Seq[Plugins] =
  Seq(play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    coverageExcludedPackages := List(
      "<empty>",
      "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.connector\\.test\\..*",
      "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.controller\\.test\\..*",
      "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.metrics\\..*",
      "uk\\.gov\\.hmrc\\.breathingspaceifproxy\\.views\\..*",
      ".*(Reverse|AuthService|BuildInfo|Routes).*"
    ).mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 96,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    PlayKeys.playDefaultPort := 9501,
    scoverageSettings,
    scalaSettings,
    libraryDependencies ++= Dependencies.all
  )
  .settings(
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-Xlint:_",
      "-Wdead-code",
      "-Wunused:_",
      "-Wextra-implicit",
      "-Ywarn-unused",
      "-Werror",
      "-Wconf:cat=unused-imports&site=.*views\\.txt:s",
      "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s"
    )
  )
  .settings(routesImport ++= Seq("uk.gov.hmrc.breathingspaceifproxy.config.Binders._"))

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Compile / unmanagedResourceDirectories += baseDirectory.value / "public"

Test / Keys.fork := true
Test / parallelExecution := false

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= Dependencies.test,
    DefaultBuildSettings.itSettings()
  )

TwirlKeys.templateImports ++= Seq()

