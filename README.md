# uuid-format

A little tool to transform between hexadecimal representations of a UUID & representation as a pair of unsigned longs.
This representation is convenient to serialize [Java UUIDs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) with, but otherwise inconvenient to work with.

This tool is also an excuse to try out writing an application with [Scala CLI](https://scala-cli.virtuslab.org/) and [decline](https://ben.kirw.in/decline/).

# Usage

```sh
scala-cli uuid-format.scala --
```
