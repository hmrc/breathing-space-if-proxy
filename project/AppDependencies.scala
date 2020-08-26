import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"  %% "bootstrap-backend-play-27"  % "2.24.0",
    "uk.gov.hmrc"  %% "domain"                     % "5.9.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "2.24.0"  % Test,
    "org.scalatest"           %% "scalatest"                % "3.1.2"   % Test,
    "com.typesafe.play"       %% "play-test"                % current   % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10" % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"   % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"             % "2.26.3"  % "test, it"
  )
}
