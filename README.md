# CLI Tools

This repository is a collection of of little Scala-CLI tools to automate various tasks. It is also an excuse to try out writing an application with [Scala CLI](https://scala-cli.virtuslab.org/) and [decline](https://ben.kirw.in/decline/).

## format-uuid

A little tool to transform between hexadecimal representations of a UUID & representation as a pair of unsigned longs.
This representation is convenient to serialize [Java UUIDs](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) with, but otherwise inconvenient to work with.

### Usage

```sh
scala-cli format-uuid/format-uuid.scala -- [details] <uuid> | <most significant bits> <least significant bits>
```

eg. 
```sh 
$ scala-cli format-uuid/format-uuid.scala -- 6c1be298-d456-11ee-b7bd-530428bbe3ed

msb: 7790069126492721646
lsb: 13239829756867503085

$ scala-cli format-uuid/format-uuid.scala -- 7790069126492721646 13239829756867503085

6c1be298-d456-11ee-b7bd-530428bbe3ed
```

The tool will accept UUIDs in the standard 8-4-4-4-12 format, or as a series of hexadecimal digits with an optional `0x` prefix.
### Building

Thanks to the magic of Scala CLI, you can create a Scala Native executable from this script without even cloning this repository using the following command:

```sh
scala-cli --power package https://github.com/reardonj/cli-tools/raw/main/src/format-uuid/format-uuid.scala -o format-uuid -f --native --native-mode release-fast --native-gc none
```