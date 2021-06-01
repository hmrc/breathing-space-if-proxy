resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "1.17.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "3.0.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "2.1.0")
addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "2.2.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.9")  // Follow the Play version

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.3")

disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
