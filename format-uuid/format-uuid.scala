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

//> using scala 3.4
//> using dep org.typelevel::cats-core::2.10.0
//> using dep com.monovore::decline::2.4.1

package com.jmreardon.uuid

import cats.*
import cats.data.*
import cats.syntax.all.*
import java.util.UUID
import com.monovore.decline._
import scala.util.*

object FormatUUID
    extends CommandApp(
      "format-uuid",
      "Transform between canonical UUID representation & unsigned long representation",
      Commands.opts.map(println)
    )

enum Commands:
  case Details(uuid: UUID)
  case Transform(args: Args)

  def evaluate: String = this match
    case Commands.Details(uuid) => s"Version: ${uuid.version()}; Variant: ${uuid.variant()}"

    case Commands.Transform(Args.FromUUID(uuid)) =>
      val msb = uuid.getMostSignificantBits()
      val lsb = uuid.getLeastSignificantBits()
      s"msb: ${msb.unsigned.show}\nlsb: ${lsb.unsigned.show}"

    case Commands.Transform(Args.FromLongs(msb, lsb)) => UUID(msb.signed, lsb.signed).show

object Commands:
  private val detailsCommand = Opts
    .subcommand("details", "show details about a UUID")(Args.uuidValueArg)
    .map {
      case Args.FromUUID(uuid)      => uuid
      case Args.FromLongs(msb, lsb) => UUID(msb.signed, lsb.signed)
    }
    .map(Commands.Details(_))

  val opts: Opts[String] = (detailsCommand orElse Args.uuidValueArg.map(Commands.Transform(_))).map(_.evaluate)

enum Args:
  case FromUUID(uuid: UUID)
  case FromLongs(msb: UnsignedLong, lsb: UnsignedLong)

object Args:
  private val uuidOpt = Opts.argument[Args.FromUUID]("uuid")

  private val longsOpt =
    (Opts.argument[UnsignedLong]("most significant bits"), Opts.argument[UnsignedLong]("least significant bits"))
      .mapN(Args.FromLongs(_, _))

  val uuidValueArg = uuidOpt orElse longsOpt

given (using uuidArg: Argument[UUID]): Argument[Args.FromUUID] with
  def read(string: String): ValidatedNel[String, Args.FromUUID] =
    val uuid = string match
      case s"0x$hex" => parseHexString(hex)
      case _         => uuidArg.read(string).findValid(parseHexString(string))
    uuid.map(Args.FromUUID(_))

  def defaultMetavar: String = "UUID"

  private def parseHexString(hex: String) = Validated
    .valid(hex)
    .ensure("UUID is wrong length")(_.length == 32)
    .ensure("UUID has invalid characters")(_.matches("""^[a-fA-F0-9]*$"""))
    .map(_.splitAt(16).bimap(java.lang.Long.parseUnsignedLong(_, 16), java.lang.Long.parseUnsignedLong(_, 16)))
    .bimap(NonEmptyList.one, { case (msb, lsb) => UUID(msb, lsb) })

/** Alias for longs that are parsed and displayed as unsigned values.
  */
opaque type UnsignedLong = Long

extension (l: Long) inline def unsigned: UnsignedLong = l
extension (l: UnsignedLong) inline def signed: Long = l

given Show[UnsignedLong] with
  def show(t: UnsignedLong): String = java.lang.Long.toUnsignedString(t)

given Argument[UnsignedLong] with
  def read(string: String): ValidatedNel[String, UnsignedLong] = Try(java.lang.Long.parseUnsignedLong(string).unsigned)
    .toValidated
    .leftMap(_ => NonEmptyList.one(s"'$string' is not a valid unsigned long"))

  def defaultMetavar: String = "unsigned long"
