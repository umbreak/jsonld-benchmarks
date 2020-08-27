/*
scalafmt: {
  style = defaultWithAlign
  maxColumn = 150
  align.preset = most
  align.tokens.add = [
    { code = ":=", owner = "Term.ApplyInfix" }
    { code = "+=", owner = "Term.ApplyInfix" }
    { code = "++=", owner = "Term.ApplyInfix" }
    { code = "~=", owner = "Term.ApplyInfix" }
  ]
  version = 2.5.3
}
 */

val scalaCompilerVersion = "2.13.2"

val circeVersion       = "0.13.0"
val jakartaJsonVersion = "1.1.6"
val jsonldjavaVersion  = "0.13.0"
val log4jVersion       = "2.1"
val scalaTestVersion   = "3.2.0"
val slf4jVersion       = "1.7.30"
val titaniumVersion    = "0.8.4"

lazy val circeParser = "io.circe"                %% "circe-parser"     % circeVersion
lazy val jakartaJson = "org.glassfish"            % "jakarta.json"     % jakartaJsonVersion
lazy val jsonldjava  = "com.github.jsonld-java"   % "jsonld-java"      % jsonldjavaVersion
lazy val scalaTest   = "org.scalatest"           %% "scalatest"        % scalaTestVersion
lazy val log4j       = "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
lazy val log4jApi    = "org.apache.logging.log4j" % "log4j-api"        % log4jVersion
lazy val log4jCore   = "org.apache.logging.log4j" % "log4j-core"       % log4jVersion
lazy val log4jJul    = "org.apache.logging.log4j" % "log4j-jul"        % log4jVersion
lazy val titanium    = "com.apicatalog"           % "titanium-json-ld" % titaniumVersion

lazy val extractSuite = taskKey[Seq[File]]("Extracts the Json-LD suite from a tar.gz")

lazy val root = project
  .in(file("."))
  .enablePlugins(JmhPlugin)
  .settings(name := "jsonLdBenchmarks", moduleName := "jsonLdBenchmarks")
  .settings(noPublish, compilation)
  .settings(
    libraryDependencies       ++= Seq(
      circeParser % Test,
      jakartaJson % Test,
      jsonldjava  % Test,
      log4j       % Test,
      log4jApi    % Test,
      log4jCore   % Test,
      log4jJul    % Test,
      scalaTest   % Test,
      titanium    % Test
    ),
    extractSuite               := {
      import org.rauschig.jarchivelib._
      import sbt._

      val log         = streams.value.log
      val archiver    = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
      val destination = (resourceDirectory in Test).value / "json-ld-11.org"
      if (!destination.exists()) {
        val source = (resourceDirectory in Test).value / "json-ld-11.org.tgz"
        archiver.extract(source, source.getParentFile)
        log.success("Extracted JSON-LD 1.1 test suite")
      }
      (destination ** "*").get
    },
    Test / resourceGenerators  += extractSuite,
    sourceDirectory in Jmh     := (sourceDirectory in Test).value,
    classDirectory in Jmh      := (classDirectory in Test).value,
    dependencyClasspath in Jmh := (dependencyClasspath in Test).value,
    compile in Jmh             := (compile in Jmh).dependsOn(compile in Test).value,
    run in Jmh                 := (run in Jmh).dependsOn(Keys.compile in Jmh).evaluated,
    Test / fork                := true,
    cleanFiles                ++= Seq((resourceDirectory in Test).value / "json-ld-11.org")
  )

lazy val noPublish = Seq(publishLocal := {}, publish := {}, publishArtifact := false)

lazy val compilation = {
  import sbt.Keys._
  import sbt._

  Seq(
    scalaVersion                     := scalaCompilerVersion,
    scalacOptions                    ~= { options: Seq[String] => options.filterNot(Set("-Wself-implicit")) },
    scalacOptions in (Compile, doc) ++= Seq("-no-link-warnings")
  )

  inThisBuild(
    Seq(
      licenses   := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      developers := List(Developer("umbreak", "Didac Montero Mendez", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")))
    )
  )
}