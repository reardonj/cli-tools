/*
  MIT License

  Copyright (c) 2024 Justin Reardon

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 */

//> using scala 3
//> using dep org.typelevel::cats-core::2.10.0
//> using dep com.monovore::decline::2.4.1

package com.jmreardon.properties

import cats.*
import cats.data.*
import cats.syntax.all.*
import java.util.UUID
import com.monovore.decline._
import scala.util.*
import scala.jdk.CollectionConverters.SetHasAsScala

import java.io.File
import java.{util => ju}
import java.nio.file.Files
import java.io.FileInputStream
import java.io.FileOutputStream

object EditProperties extends CommandApp("edit-properties", "Update Java properties files", Commands.all)

object Commands:
  final class LoadException(file: String, cause: Throwable)
      extends Exception(s"Error loading file '$file': ${cause.getMessage()}", cause)

  final class SaveException(file: String, cause: Throwable)
      extends Exception(s"Error saving file '$file': ${cause.getMessage()}", cause)

  def all = Seq(
    Opts.subcommand("copy-keys", "copy any keys missing in the destination files.")(copyKeys),
    Opts.subcommand("rename-key", "rename key")(renameKey)
  ).reduce(_ orElse _)
    .map(
      _.fold(
        throwable =>
          System.err.println(throwable.getMessage())
          System.exit(-1)
        ,
        _ => ()
      )
    )

  val source = Opts.option[String]("source", "source file", "s").map(loadProperties)

  val destinations = Opts.flag("destination", "destination files", "d") *>
    Opts.arguments[String]("destination files").map(_.toList.traverse(loadProperties))

  val copyKeys = (source, destinations).mapN { (source, destinations) =>
    (source, destinations).flatMapN { case ((_, source), destinations) =>
      val sourceKeys = Set.from(source.stringPropertyNames().asScala)

      destinations
        .flatMap { case (file, destination) =>
          val missing = sourceKeys -- Set.from(destination.stringPropertyNames().asScala)
          missing.foreach { missing => destination.setProperty(missing, "<COPY>" + source.getProperty(missing)) }

          Option.when(missing.nonEmpty)((file, destination))
        }
        .traverse_(saveProperties)
    }
  }

  val renameKey =
    (Opts.option[String]("from", "current key name", "f"), Opts.option[String]("to", "new key name", "t"), destinations)
      .mapN { (from, to, destinations) =>
        destinations.flatMap {
          _.flatMap { case (file, destination) =>
              val matchingKeys = Set.from(destination.stringPropertyNames().asScala).filter(_.startsWith(from))
              matchingKeys.foreach { originalKey =>
                destination.setProperty(originalKey.replace(from, to), destination.getProperty(originalKey))
                destination.remove(originalKey)
              }

              Option.when(matchingKeys.nonEmpty)((file, destination))
            }
            .traverse_(saveProperties)
        }
      }

  private def loadProperties(file: String): Try[(String, ju.Properties)] = Using(new FileInputStream(file)) { input =>
    val props = ju.Properties()
    props.load(input)
    file -> props
  }.adaptError(LoadException(file, _))

  private def saveProperties(file: String, properties: ju.Properties): Try[Unit] = Using(new FileOutputStream(file)) {
    output => properties.store(output, "edit-properties")
  }.adaptError(SaveException(file, _))
