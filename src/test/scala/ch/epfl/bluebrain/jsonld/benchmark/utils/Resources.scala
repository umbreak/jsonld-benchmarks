package ch.epfl.bluebrain.jsonld.benchmark.utils

import io.circe.Json
import io.circe.parser.parse

import scala.io.Codec.UTF8
import scala.io.Source

/**
  * Utility trait that facilitates operating on classpath resources.
  */
trait Resources {

  private val codec = UTF8

  final def contentOf(resourcePath: String): String = {
    lazy val fromClass       = Option(getClass.getResourceAsStream(resourcePath))
    lazy val fromClassLoader = Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
    val is                   = (fromClass orElse fromClassLoader).getOrElse(
      throw new IllegalArgumentException(s"Unable to load resource '$resourcePath' from classpath.")
    )
    Source.fromInputStream(is)(codec).mkString
  }

  final def jsonContentOf(resourcePath: String): Json =
    parse(contentOf(resourcePath)).toTry.get

}

object Resources extends Resources
