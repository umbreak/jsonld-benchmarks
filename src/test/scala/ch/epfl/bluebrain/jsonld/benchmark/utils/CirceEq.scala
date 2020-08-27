package ch.epfl.bluebrain.jsonld.benchmark.utils

import io.circe._
import io.circe.syntax._
import org.scalatest.matchers.{MatchResult, Matcher}

trait CirceEq {
  implicit private val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def equalIgnoreArrayOrder(json: Json): Matcher[Json] = new IgnoredArrayOrder(json)

  private class IgnoredArrayOrder(json: Json) extends Matcher[Json] {
    private def sortKeys(value: Json): Json = {
      def canonicalJson(json: Json): Json =
        json.arrayOrObject[Json](
          json,
          arr =>
            Json.fromValues(
              arr.map(canonicalJson).sortBy(_.hashCode)
            ),
          obj => sorted(obj).asJson
        )

      def sorted(jObj: JsonObject): JsonObject =
        JsonObject.fromIterable(jObj.toVector.sortBy(_._1).map { case (k, v) => k -> canonicalJson(v) })

      canonicalJson(value)
    }

    override def apply(left: Json): MatchResult = {
      lazy val leftSortedByJson  = sortKeys(left)
      lazy val rightSortedByJson = sortKeys(json)
      MatchResult(
          (leftSortedByJson == rightSortedByJson) ||
          (printer.print(leftSortedByJson) == printer.print(rightSortedByJson)),
        s"Both Json are not equal (ignoring array order)\n${printer.print(leftSortedByJson)}\ndid not equal\n${printer
          .print(rightSortedByJson)}",
        ""
      )
    }
  }

}
