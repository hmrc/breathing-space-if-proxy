import sbt.*

object Dependencies {
  private val playVersion = "play-30"
  private val mongoVersion = "2.6.0"
  private val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "org.typelevel"     %% "cats-core"                       % "2.13.0",
    "com.beachape"      %% "enumeratum"                      % "1.7.5",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"        % mongoVersion,
    "commons-codec"     % "commons-codec"                    % "1.18.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.scalatestplus" %% "scalacheck-1-18"               % "3.2.19.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % mongoVersion
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
