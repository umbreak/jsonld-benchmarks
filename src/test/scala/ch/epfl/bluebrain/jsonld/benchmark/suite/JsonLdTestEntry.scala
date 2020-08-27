package ch.epfl.bluebrain.jsonld.benchmark.suite

import ch.epfl.bluebrain.jsonld.benchmark.JsonLdTestExecutor
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestEntry.Opts
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestSuite.{BaseIri, SUITE}
import ch.epfl.bluebrain.jsonld.benchmark.utils.Resources
import io.circe.{Decoder, Json, JsonObject}

sealed trait JsonLdTestEntry extends Product with Serializable with Resources {
  type I
  type O

  def base: String
  def inputPath: String
  def expectPath: String
  def opts: Opts
  def input: I
  def expect: O
  def run[A](implicit exec: JsonLdTestExecutor[A]): O
  def hasSpecVersion10: Boolean = opts.get("specVersion").contains(Json.fromString("json-ld-1.0"))
}

object JsonLdTestEntry {
  type Opts = Map[String, Json]

  final case class ExpandTestEntry(inputPath: String, expectPath: String, opts: Opts, base: String)
      extends JsonLdTestEntry {
    type I = Json
    type O = Json

    lazy val input: I                                   = jsonContentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = jsonContentOf(s"/$SUITE/$expectPath")
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)
  }

  final case class FlattenTestEntry(
      inputPath: String,
      contextPath: Option[String],
      expectPath: String,
      opts: Opts,
      base: String
  ) extends JsonLdTestEntry {
    type I = Json
    type O = Json

    lazy val input: I                                   = jsonContentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = jsonContentOf(s"/$SUITE/$expectPath")
    def context: Option[Json]                           = contextPath.map(cp => jsonContentOf(s"/$SUITE/$cp"))
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)

  }

  final case class ToRdfTestEntry(inputPath: String, expectPath: String, opts: Opts, base: String)
      extends JsonLdTestEntry {
    type I = Json
    type O = String

    lazy val input: I                                   = jsonContentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = contentOf(s"/$SUITE/$expectPath")
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)
  }

  final case class FromRdfTestEntry(inputPath: String, expectPath: String, opts: Opts, base: String)
      extends JsonLdTestEntry {
    type I = String
    type O = Json

    lazy val input: I                                   = contentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = jsonContentOf(s"/$SUITE/$expectPath")
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)

  }

  final case class CompactTestEntry(
      inputPath: String,
      contextPath: String,
      expectPath: String,
      opts: Opts,
      base: String
  ) extends JsonLdTestEntry {
    type I = Json
    type O = Json

    lazy val input: I                                   = jsonContentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = jsonContentOf(s"/$SUITE/$expectPath")
    lazy val context: Json                              = jsonContentOf(s"/$SUITE/$contextPath")
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)

  }

  final case class FrameTestEntry(inputPath: String, framePath: String, expectPath: String, opts: Opts, base: String)
      extends JsonLdTestEntry {
    type I = Json
    type O = Json

    lazy val input: I                                   = jsonContentOf(s"/$SUITE/$inputPath")
    lazy val expect: O                                  = jsonContentOf(s"/$SUITE/$expectPath")
    lazy val frame: Json                                = jsonContentOf(s"/$SUITE/$framePath")
    def run[A](implicit exec: JsonLdTestExecutor[A]): O = exec(this)
  }

  implicit def jsonLdTestEntryDecoder(implicit baseParent: BaseIri): Decoder[Option[JsonLdTestEntry]] =
    Decoder.instance { hc =>
      for {
        input     <- hc.get[String]("input")
        types     <- hc.get[List[String]]("@type")
        expectOpt <- hc.get[Option[String]]("expect")
        context   <- hc.get[Option[String]]("context")
        frame     <- hc.get[Option[String]]("frame")
        optObj    <- hc.get[Option[JsonObject]]("option")
        base       = s"${baseParent.value}$input"
        opt        = optObj.map(_.toMap).getOrElse(Map.empty)
      } yield (types, context, frame, expectOpt) match {
        case ("jld:NegativeEvaluationTest" :: _, _, _, _)                   =>
          None
        case (_, _, _, None)                                                =>
          None
        case (_ :: "jld:ExpandTest" :: Nil, None, None, Some(expect))       =>
          Some(ExpandTestEntry(input, expect, opt, base))
        case (_ :: "jld:FromRDFTest" :: Nil, None, None, Some(expect))      =>
          Some(FromRdfTestEntry(input, expect, opt, base))
        case (_ :: "jld:CompactTest" :: Nil, Some(ctx), None, Some(expect)) =>
          Some(CompactTestEntry(input, ctx, expect, opt, base))
        case (_ :: "jld:FlattenTest" :: Nil, ctxOpt, None, Some(expect))    =>
          Some(FlattenTestEntry(input, ctxOpt, expect, opt, base))
        case (_ :: "jld:FrameTest" :: Nil, None, Some(frame), Some(expect)) =>
          Some(FrameTestEntry(input, frame, expect, opt, base))
        case (_ :: "jld:ToRDFTest" :: Nil, None, None, Some(expect))        =>
          Some(ToRdfTestEntry(input, expect, opt, base))
        case _                                                              =>
          throw new IllegalArgumentException(s"None of the expected entry tests with types '$types'")
      }
    }
}
