package ch.epfl.bluebrain.jsonld.benchmark

import java.io.{ByteArrayOutputStream, StringReader}
import java.net.URI

import ch.epfl.bluebrain.jsonld.benchmark.JsonLdImplementations.{JsonLdJava, Titanium}
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestEntry._
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestEntry
import com.apicatalog.jsonld.JsonLd
import com.apicatalog.jsonld.JsonLdOptions.RdfDirection
import com.apicatalog.jsonld.document.{Document, JsonDocument, RdfDocument}
import com.apicatalog.jsonld.JsonLdVersion
import com.github.jsonldjava.core.{JsonLdConsts, JsonLdProcessor}
import com.github.jsonldjava.utils.JsonUtils
import io.circe.Json
import io.circe.parser.parse
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.rdf.Rdf

sealed trait JsonLdImplementations extends Product with Serializable

object JsonLdImplementations {
  type Titanium   = Titanium.type
  type JsonLdJava = JsonLdJava.type
  final case object Titanium   extends JsonLdImplementations
  final case object JsonLdJava extends JsonLdImplementations
}

trait JsonLdTestExecutor[A] {
  def apply(entry: ExpandTestEntry): entry.O
  def apply(entry: CompactTestEntry): entry.O
  def apply(entry: FlattenTestEntry): entry.O
  def apply(entry: FrameTestEntry): entry.O
  def apply(entry: ToRdfTestEntry): entry.O
  def apply(entry: FromRdfTestEntry): entry.O
}

object JsonLdTestExecutor {

  private def parseUnsafe(str: String): Json =
    parse(str).toOption.get

  implicit val titaniumExecutor: JsonLdTestExecutor[Titanium] = new JsonLdTestExecutor[Titanium] {
    import com.apicatalog.jsonld.JsonLdOptions

    //TODO: Hack while the issue with overridden ops base to context base exists
    private def hasValidBase(entry: JsonLdTestEntry): Boolean = {
      val jsonOpt = entry match {
        case e: CompactTestEntry => Some(e.context)
        case e: ToRdfTestEntry   => Some(e.input)
        case _                   => None
      }
      jsonOpt match {
        case Some(json) =>
          json.hcursor.downField("@context").get[String]("@base") match {
            case Right(iri) => iri.startsWith("http")
            case _          => false
          }
        case _          => false
      }

    }

    implicit private def jsonldOpts(entry: JsonLdTestEntry): JsonLdOptions = {
      val ops = new JsonLdOptions()
      entry.opts.get("produceGeneralizedRdf").flatMap(_.asBoolean).foreach(ops.setProduceGeneralizedRdf)
      entry.opts.get("processingMode").flatMap(_.asString).foreach(v => ops.setProcessingMode(JsonLdVersion.of(v)))
      entry.opts.get("omitGraph").flatMap(_.asBoolean).foreach(ops.setOmitGraph(_))
      entry.opts.get("compactArrays").flatMap(_.asBoolean).foreach(ops.setCompactArrays)
      entry.opts.get("useNativeTypes").flatMap(_.asBoolean).foreach(ops.setUseNativeTypes)
      entry.opts.get("useRdfType").flatMap(_.asBoolean).foreach(ops.setUseRdfType)
      entry.opts
        .get("rdfDirection")
        .flatMap(_.asString)
        .foreach(v => ops.setRdfDirection(RdfDirection.valueOf(v.replace('-', '_').toUpperCase())))
      entry.opts.get("expandContext").flatMap(_.asString).foreach(ops.setExpandContext)
      if (!hasValidBase(entry)) {
        val base = entry.opts.get("base").flatMap(_.asString).getOrElse(entry.base)
        ops.setBase(URI.create(base))
      }
      ops
    }

    implicit private def jsonToTitanium(json: Json): Document =
      JsonDocument.of(new StringReader(json.noSpaces))

    private def strToRdfTitanium(str: String): Document =
      RdfDocument.of(new StringReader(str))

    override def apply(entry: ExpandTestEntry): entry.O =
      parseUnsafe(JsonLd.expand(entry.input).options(entry).get().toString)

    override def apply(entry: CompactTestEntry): entry.O =
      parseUnsafe(JsonLd.compact(entry.input, entry.context).options(entry).get().toString)

    override def apply(entry: FlattenTestEntry): entry.O =
      entry.context match {
        case Some(ctx) => parseUnsafe(JsonLd.flatten(entry.input).context(ctx).options(entry).get().toString)
        case None      => parseUnsafe(JsonLd.flatten(entry.input).options(entry).get().toString)
      }

    override def apply(entry: FrameTestEntry): entry.O =
      parseUnsafe(JsonLd.frame(entry.input, entry.frame).options(entry).get().toString)

    override def apply(entry: ToRdfTestEntry): entry.O = {
      val os      = new ByteArrayOutputStream()
      val dataset = JsonLd.toRdf(entry.input).options(entry).get()
      Rdf.createWriter(MediaType.N_QUADS, os).write(dataset)
      os.toString
    }

    override def apply(entry: FromRdfTestEntry): entry.O =
      parseUnsafe(JsonLd.fromRdf(strToRdfTitanium(entry.input)).options(entry).get().toString)
  }

  implicit val jsonLdJavaExecutor: JsonLdTestExecutor[JsonLdJava] = new JsonLdTestExecutor[JsonLdJava] {
    import com.github.jsonldjava.core.JsonLdOptions

    implicit private def jsonldOpts(entry: JsonLdTestEntry): JsonLdOptions = {
      val ops  = new JsonLdOptions()
      entry.opts.get("produceGeneralizedRdf").flatMap(_.asBoolean).foreach(ops.setProduceGeneralizedRdf(_))
      entry.opts
        .get("processingMode")
        .orElse(entry.opts.get("specVersion"))
        .flatMap(_.asString)
        .foreach(ops.setProcessingMode)
      entry.opts.get("omitGraph").flatMap(_.asBoolean).foreach(ops.setOmitGraph(_))
      entry.opts.get("compactArrays").flatMap(_.asBoolean).foreach(ops.setCompactArrays(_))
      entry.opts.get("useNativeTypes").flatMap(_.asBoolean).foreach(ops.setUseNativeTypes(_))
      entry.opts.get("useRdfType").flatMap(_.asBoolean).foreach(ops.setUseRdfType(_))
      entry.opts.get("expandContext").flatMap(_.asString).foreach(ops.setExpandContext)
      val base = entry.opts.get("base").flatMap(_.asString).getOrElse(entry.base)
      ops.setBase(base)
      ops
    }

    override def apply(entry: ExpandTestEntry): entry.O = {
      val obj = JsonUtils.fromString(entry.input.noSpaces)
      parseUnsafe(JsonUtils.toString(JsonLdProcessor.expand(obj, entry)))
    }

    override def apply(entry: CompactTestEntry): entry.O = {
      val obj = JsonUtils.fromString(entry.input.noSpaces)
      val ctx = JsonUtils.fromString(entry.context.noSpaces)
      parseUnsafe(JsonUtils.toString(JsonLdProcessor.compact(obj, ctx, entry)))
    }

    override def apply(entry: FlattenTestEntry): entry.O = {
      val obj = JsonUtils.fromString(entry.input.noSpaces)
      entry.context match {
        case Some(ctxJson) =>
          val ctx = JsonUtils.fromString(ctxJson.noSpaces)
          parseUnsafe(JsonUtils.toString(JsonLdProcessor.flatten(obj, ctx, entry)))
        case None          =>
          parseUnsafe(JsonUtils.toString(JsonLdProcessor.flatten(obj, entry)))

      }
    }

    override def apply(entry: FrameTestEntry): entry.O = {
      val obj   = JsonUtils.fromString(entry.input.noSpaces)
      val frame = JsonUtils.fromString(entry.frame.noSpaces)
      parseUnsafe(JsonUtils.toString(JsonLdProcessor.frame(obj, frame, entry)))
    }

    override def apply(entry: ToRdfTestEntry): entry.O = {
      val obj  = JsonUtils.fromString(entry.input.noSpaces)
      val opts = jsonldOpts(entry)
      opts.format = JsonLdConsts.APPLICATION_NQUADS
      JsonLdProcessor.toRDF(obj, opts).toString
    }

    override def apply(entry: FromRdfTestEntry): entry.O = {
      parseUnsafe(JsonUtils.toString(JsonLdProcessor.fromRDF(entry.input, entry)))
    }
  }
}
