import play.core.PlayVersion.current
import sbt._

object Dependencies {

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-27" % "2.25.0",
    "org.typelevel" %% "cats-core"                 % "2.2.0",
    "com.beachape"  %% "enumeratum"                % "1.6.1",
    "com.kenshoo"   %% "metrics-play"              % "2.7.3_0.8.2"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-27"   % "2.25.0"  % Test,
    "com.typesafe.play"      %% "play-test"                % current   % Test,
    "org.scalatest"          %% "scalatest"                % "3.2.2"   % Test,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.35.10" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "4.0.3"   % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"            % "2.26.3"  % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.15.0"  % "test, it"
  )
}
