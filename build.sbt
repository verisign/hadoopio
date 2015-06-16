homepage := Some(url("https://github.com/verisign/hadoopio"))

licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))

organization := "com.verisign.hadoopio"

name := "hadoopio"

resolvers ++= Seq(
  "typesafe-repository" at "https://repo.typesafe.com/typesafe/releases/",
  // http://www.cloudera.com/content/cloudera/en/documentation/cdh4/latest/CDH4-Installation-Guide/cdh4ig_using_Maven.html
  "cloudera-repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)


// -------------------------------------------------------------------------------------------------------------------
// Variables
// -------------------------------------------------------------------------------------------------------------------

val avroVersion = "1.7.7"
// See http://www.cloudera.com/content/cloudera/en/documentation/cdh4/latest/CDH-Version-and-Packaging-Information/cdhvd_topic_8.html
val hadoopVersion = "2.5.0-cdh5.2.1"
val hadoopMapReduceVersion = "2.5.0-mr1-cdh5.2.1"
val javaVersion = "1.7"
val logbackVersion = "1.1.2"
val mainScalaVersion = "2.10.4"


// -------------------------------------------------------------------------------------------------------------------
// Dependencies
// -------------------------------------------------------------------------------------------------------------------

// Main dependencies
libraryDependencies ++= Seq(
  "org.apache.avro" % "avro-mapred" % avroVersion,
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion,
  // Contains e.g. compression codecs
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion
    exclude("org.slf4j", "slf4j-log4j12"),
  // Logback with slf4j facade
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

// Required IntelliJ workaround.  This tells `sbt gen-idea` to include scala-reflect as a compile dependency (and not
// merely as a test dependency), which we need for TypeTag usage.
libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)

// Test dependencies
libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "14.0.1" % "test",
  // The 'classifier "tests"' modifier is required to pull in the hadoop-hdfs tests jar, which contains MiniDFSCluster.
  "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion % "test" classifier "tests",
  // Required for e.g. org.apache.hadoop.net.StaticMapping
  "org.apache.hadoop" % "hadoop-common" % hadoopVersion % "test" classifier "tests",
  "org.apache.hadoop" % "hadoop-minicluster" % hadoopMapReduceVersion % "test",
  "org.scalatest" %% "scalatest" % "2.2.2" % "test"
)

dependencyOverrides += "com.google.guava" % "guava" % "14.0.1"

// -------------------------------------------------------------------------------------------------------------------
// Avro
// -------------------------------------------------------------------------------------------------------------------
seq(sbtavro.SbtAvro.avroSettings : _*)

// Configure the desired Avro version.  sbt-avro automatically injects a libraryDependency.
(version in avroConfig) := avroVersion

// Look for *.avsc etc. files in src/main/resources/avro/
(sourceDirectory in avroConfig) <<= (sourceDirectory in Compile)(_ / "resources/avro")

(stringType in avroConfig) := "String"


// -------------------------------------------------------------------------------------------------------------------
// Compiler and JVM settings
// -------------------------------------------------------------------------------------------------------------------

crossScalaVersions := Seq(mainScalaVersion, "2.11.6")

scalaVersion := mainScalaVersion

// Enable forking (see sbt docs) because our full build (including tests) uses many threads.
fork := true

// The following options are passed to forked JVMs.
//
// Note: If you need to pass options to the JVM used by sbt (i.e. the "parent" JVM), then you should modify `.sbtopts`.
javaOptions ++= Seq(
  "-Xmx256m",
  "-XX:+UseG1GC",
  "-Djava.awt.headless=true",
  "-Djava.net.preferIPv4Stack=true")

javacOptions in Compile ++= Seq(
  "-source", javaVersion,
  "-target", javaVersion,
  "-Xlint:deprecation")

scalacOptions ++= Seq(
  "-target:jvm-" + javaVersion,
  "-encoding", "UTF-8"
)

scalacOptions in Compile ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature",  // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

scalacOptions in Test ~= { (options: Seq[String]) =>
  options.filterNot(_ == "-Ywarn-value-discard").filterNot(_ == "-Ywarn-dead-code" /* to fix warnings due to Mockito */)
}

scalacOptions in ScoverageTest ~= { (options: Seq[String]) =>
  options.filterNot(_ == "-Ywarn-value-discard").filterNot(_ == "-Ywarn-dead-code" /* to fix warnings due to Mockito */)
}


// -------------------------------------------------------------------------------------------------------------------
// Testing
// -------------------------------------------------------------------------------------------------------------------

parallelExecution in ThisBuild := false

// Write test results to file in JUnit XML format
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports/junitxml")

// Write test results to console.
//
// Tip: If you need to troubleshoot test runs, it helps to use the following reporting setup for ScalaTest.
//      Notably these suggested settings will ensure that all test output is written sequentially so that it is easier
//      to understand sequences of events, particularly cause and effect.
//      (cf. http://www.scalatest.org/user_guide/using_the_runner, section "Configuring reporters")
//
//        testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oUDT", "-eUDT")
//
//        // This variant also disables ANSI color output in the terminal, which is helpful if you want to capture the
//        // test output to file and then run grep/awk/sed/etc. on it.
//        testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oWUDT", "-eWUDT")
//
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-o")

// https://github.com/scoverage/scalac-scoverage-plugin
instrumentSettings


// ---------------------------------------------------------------------------------------------------------------
// Scaladoc settings
// ---------------------------------------------------------------------------------------------------------------
(scalacOptions in doc) <++= (name, version).map { (n, v) => Seq("-doc-title", n, "-doc-version", v) }

// https://github.com/sbt/sbt-unidoc
unidocSettings


// -------------------------------------------------------------------------------------------------------------------
// Releasing
// -------------------------------------------------------------------------------------------------------------------
publishMavenStyle := true

publishArtifact in Test := false

// TODO: Add integration with SonaType to publish to Maven Central under `com.verisign`.

pomIncludeRepository := { _ => false }

pomExtra :=
  <scm>
    <url>https://github.com/verisign/hadoopio/</url>
    <connection>scm:git:git@github.com:verisign/hadoopio.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mnoll</id>
      <name>Michael G. Noll</name>
      <email>mnoll@verisign.com</email>
      <timezone>Europe/Zurich</timezone>
    </developer>
  </developers>


// ---------------------------------------------------------------------------------------------------------------------
// Misc settings
// ---------------------------------------------------------------------------------------------------------------------
// https://github.com/jrudolph/sbt-dependency-graph
net.virtualvoid.sbt.graph.Plugin.graphSettings
