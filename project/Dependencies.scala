import play.core.PlayVersion
import sbt._

object Dependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-28" % "5.14.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.12.3",
    "org.typelevel"                %% "cats-core"                 % "2.6.1",
    "com.beachape"                 %% "enumeratum"                % "1.6.1",
    "com.kenshoo"                  %% "metrics-play"              % "2.7.3_0.8.2",
    "ai.x"                         %% "play-json-extensions"      % "0.42.0",
    "uk.gov.hmrc"                  %% "reactive-circuit-breaker"  % "3.5.0"

  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % "5.3.0"    % Test,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.16.37"  % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.9"    % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.36.8"   % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"    % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.28.0"   % "test, it"
  )
}
