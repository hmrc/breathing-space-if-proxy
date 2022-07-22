import play.core.PlayVersion
import sbt._

object Dependencies {

  val compile = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-28" % "6.3.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % "2.13.3",
    "org.typelevel"                %% "cats-core"                 % "2.7.0",
    "com.beachape"                 %% "enumeratum"                % "1.7.0",
    "com.kenshoo"                  %% "metrics-play"              % "2.7.3_0.8.2",
    "ai.x"                         %% "play-json-extensions"      % "0.42.0",
    "uk.gov.hmrc"                  %% "reactive-circuit-breaker"  % "3.5.0",
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"        % "0.68.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"   % "6.3.0"    % Test,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.7"   % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.12"   % Test,
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.11.0" % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.62.2"   % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"    % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.33.2"   % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"  % "0.68.0"   % "test, it"
  )
}
