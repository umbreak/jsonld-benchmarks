package ch.epfl.bluebrain.jsonld.benchmark.suite

import java.util.Locale

import ch.epfl.bluebrain.jsonld.benchmark.JsonLdImplementations.{JsonLdJava, Titanium}
import ch.epfl.bluebrain.jsonld.benchmark.suite.JsonLdTestSuite._
import ch.epfl.bluebrain.jsonld.benchmark.utils.{CirceEq, Resources}
import io.circe.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, OptionValues}

class JsonLdTestSuiteSpec
    extends AnyWordSpecLike
    with Matchers
    with Resources
    with OptionValues
    with CirceEq
    with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    Locale.setDefault(Locale.US)
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
  }

  private def sortedSeq(result: String) =
    result.split("\n").map(_.replace("_:c14n0", "_:b0").replace("_:l1", "_:b0").replace("_:l2", "_:b1")).sorted

  "A json-ld test suite" should {

    "run with Json-LD java implementation" in {
      val entries = JsonLdTestSuite(excludeJsonLdJava ++ excludeRemoteContext)
      entries.foreach { entry =>
        (entry.run[JsonLdJava], entry.expect) match {
          case (result: Json, expect: Json)     => result should equalIgnoreArrayOrder(expect)
          case (result: String, expect: String) => result shouldEqual expect
          case _                                => fail()
        }
      }
    }

    "run with Titanium implementation" in {
      val entries = JsonLdTestSuite(excludeTitanium ++ excludePassedTitaniumButWrongEquality ++ excludeRemoteContext)
      entries.foreach { entry =>
        (entry.run[Titanium], entry.expect) match {
          case (result: Json, expect: Json)     => result should equalIgnoreArrayOrder(expect)
          case (result: String, expect: String) =>
            sortedSeq(result) shouldEqual sortedSeq(expect)
          case _                                => fail()
        }
      }
    }
  }
}
