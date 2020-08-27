package ch.epfl.bluebrain.jsonld.benchmark

import java.util.Locale

import ch.epfl.bluebrain.jsonld.benchmark.JsonLdImplementations.{JsonLdJava, Titanium}
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestSuite._
import ch.epfl.bluebrain.jsonld.benchmark.suite.{JsonLdTestEntry, JsonLdTestSuite}
import ch.epfl.bluebrain.jsonld.benchmark.utils.Resources
import org.openjdk.jmh.annotations._
import org.scalatest.OptionValues

//noinspection TypeAnnotation
/**
  * Benchmark on JsonLdImplementations: Titanium vs Json-LD Java
  * To run it, execute on the sbt shell: ''jmh:run''
  */
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@Threads(1)
class JsonLdImplementationsBenchmark {

  @Benchmark
  def compactJsonLdJava(suite: Suite): Unit = suite.compact.foreach(_.run[JsonLdJava])

  @Benchmark
  def compactTitanium(suite: Suite): Unit = suite.compact.foreach(_.run[Titanium])

  @Benchmark
  def expandJsonLdJava(suite: Suite): Unit = suite.expand.foreach(_.run[JsonLdJava])

  @Benchmark
  def expandTitanium(suite: Suite): Unit = suite.expand.foreach(_.run[Titanium])

  @Benchmark
  def flattenJsonLdJava(suite: Suite): Unit = suite.flatten.foreach(_.run[JsonLdJava])

  @Benchmark
  def flattenTitanium(suite: Suite): Unit = suite.flatten.foreach(_.run[Titanium])

  @Benchmark
  def frameJsonLdJava(suite: Suite): Unit = suite.frame.foreach(_.run[JsonLdJava])

  @Benchmark
  def frameTitanium(suite: Suite): Unit = suite.frame.foreach(_.run[Titanium])

  @Benchmark
  def fromRdfJsonLdJava(suite: Suite): Unit = suite.fromRdf.foreach(_.run[JsonLdJava])

  @Benchmark
  def fromRdfTitanium(suite: Suite): Unit = suite.fromRdf.foreach(_.run[Titanium])

  @Benchmark
  def toRdfJsonLdJava(suite: Suite): Unit = suite.toRdf.foreach(_.run[JsonLdJava])

  @Benchmark
  def toRdfTitanium(suite: Suite): Unit = suite.toRdf.foreach(_.run[Titanium])

}

@State(Scope.Benchmark)
class Suite extends Resources with OptionValues {

  Locale.setDefault(Locale.US)
  System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  private val excluded = excludeJsonLdJava ++ excludeRemoteContext ++ excludeTitanium

  // compact.size = 89 entries
  val compact: Seq[JsonLdTestEntry] = JsonLdTestSuite("compact", excluded)
  // expand.size = 83 entries
  val expand: Seq[JsonLdTestEntry]  = JsonLdTestSuite("expand", excluded)
  // flatten.size = 45 entries
  val flatten: Seq[JsonLdTestEntry] = JsonLdTestSuite("flatten", excluded)
  // frame.size = 42 entries
  val frame: Seq[JsonLdTestEntry]   = JsonLdTestSuite("frame", excluded)
  // fromRdf.size = 30 entries
  val fromRdf: Seq[JsonLdTestEntry] = JsonLdTestSuite("fromRdf", excluded)
  // fromRdf.size = 127 entries
  val toRdf: Seq[JsonLdTestEntry]   = JsonLdTestSuite("toRdf", excluded)
}
