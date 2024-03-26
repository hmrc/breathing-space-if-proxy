import sbt.*

object Dependencies {
  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-backend-$playVersion" % "8.5.0",
    "org.typelevel" %% "cats-core" % "2.10.0",
    "com.beachape" %% "enumeratum" % "1.7.3",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % "1.8.0",
    "commons-codec" % "commons-codec" % "1.16.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % "8.5.0",
    "org.mockito" %% "mockito-scala-scalatest" % "1.17.30",
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % "1.8.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
