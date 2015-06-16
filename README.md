# HadoopIO

Scala/Java library to conveniently interact with files (notably Avro files) stored in Hadoop HDFS.


---

Table of Contents

* <a href="#quickstart">Show me!</a>
* <a href="#motivation">Motivation</a>
* <a href="#usage">Usage</a>
* <a href="#philosophy">Philosophy</a>
* <a href="#development">Development</a>
* <a href="#changelog">Change log</a>
* <a href="#Contributing">Contributing</a>
* <a href="#Authors">Authors</a>
* <a href="#License">License</a>
* <a href="#References">References</a>

---


<a name="quickstart"></a>

# Show me!

Here's how you would read Avro files stored in HDFS.

Think:

```bash
# Shell -- note that this command line does not actually work. :-)
$ hadoop fs -cat /path/to/file.avro | java -jar avro-tools.jar tojson | head -n 5

# HadoopIO
AvroFsReader(cfg).read("/path/to/file.avro") take 5 foreach println
```

First we start the Scala REPL:

```bash
# Must be run from the top-level directory of this git repository.
$ ./sbt console
```

Then we read and process some Avro data:

> In this REPL example we will actually read local files because HadoopIO does not (yet?) ship with a, say, dockerized
> HDFS cluster setup -- but the code in this example is identical to HDFS usage.

```scala
scala> import org.apache.avro.generic.GenericRecord
scala> import com.verisign.hadoopio.avro._

// This configuration object makes HadoopIO aware of the relevant HDFS cluster.
// If it cannot find any Hadoop configuration files in the Java classpath (like
// in this demo), it will by default read from the local filesystem.
scala> val cfg = new org.apache.hadoop.hdfs.HdfsConfiguration

// `input` is typically an HDFS path,  but for this demo we use a local file.
// `input` could also point to a directory containing multiple such files.
scala> val input = new org.apache.hadoop.fs.Path("src/test/resources/avro/tweets1.avro")

// Enter HadoopIO
scala> val reader = AvroFsReader(cfg)

// Now we read the Avro data, i.e. here is what you came looking for.
scala> val itr: Iterator[GenericRecord] = reader.read(input)

// `itr` is a full-fledged Scala iterator!
scala> itr take 2 foreach println
// >>> {"username": "miguno", "tweet": "Rock: Nerf paper, scissors is fine.", "timestamp": 1366150681}
// >>> {"username": "BlizzardCS", "tweet": "Works as intended.  Terran is IMBA.", "timestamp": 1366154481}

scala> itr take 2 foreach { record => println(record.get("username")) }
DarkTemplar
VoidRay

// Less efficient than necessary but we like to showcase some monad love...
scala> itr take 4 map { record => record.get("tweet").toString } map { _.toUpperCase } foreach println
FIRE AT WILL, COMMANDER.
I AM THE BLADE OF SHAKURAS!
I RETURN TO SERVE!
EN TARO ADUN!

// Remember that an iterator can only be traversed once!
// Create a new one if needed.
scala> val itr2: Iterator[GenericRecord] = reader.read(input)
// `size` must traverse the iterator completely...
scala> itr2.size
res9: Int = 10
// ...so calling `size` a second time will return 0.
scala> itr2.size
res10: Int = 0
```

> Avro caveat: If, while playing around with Avro's GenericRecord, you run into
> "ClassCastException: org.apache.avro.util.Utf8 cannot be cast to java.lang.String",
> simply convert the `Utf8` value into a `String` via `obj.toString`.

We also support Avro's specific API, which requires access to the data's Avro schema (here:
[twitter.avsc](src/main/resources/avro/twitter.avsc)) and the derived Java classes (here:
`Tweet`):

```scala
val specItr: Iterator[Tweet] = reader.readSpecific[Tweet](input)
// Notice how we can now access the `getUsername` method on a tweet
// instead of having to call `record.get("username")`.
specItr.foreach { tweet => println(tweet.getUsername) }
```


<a name="motivation"></a>

# Motivation

HadoopIO was created to simplify fetching small-scale HDFS data, notably data that is stored in Avro format.
It was not intended for large-scale, distributed HDFS I/O.  At the moment, reading an HDFS directory via
[AvroFsReader](src/main/scala/com/verisign/hadoopio/avro/AvroFsReader.scala) will translate into a
single-threaded, record-by-record READ operation of the provided input path.

An example use case is a data pipeline built with Apache Storm, where bolt instances will periodically retrieve Avro
data from HDFS that was generated from a batch processing job.


<a name="usage"></a>

# Usage

## Adding HadoopIO to your build

### Maven repositories

**TODO:** The artifacts are not yet published to Maven Central, but it's on our todo list!


### HadoopIO dependency

sbt:

```scala
libraryDependencies += "com.verisign.hadoopio" % "hadoopio_2.10" % "0.2.0-SNAPSHOT"
```

Gradle:

```gradle
compile 'com.verisign.hadoopio:hadoopio_2.10:0.2.0-SNAPSHOT'
```

Maven:

```xml
<dependency>
  <groupId>com.verisign.hadoopio</groupId>
  <artifactId>hadoopio_2.10</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```


<a name="philosophy"></a>

# Philosophy and how it works

HadoopIO is intended to provide _monadic access_ to Avro data coupled with _lazy evaluation_.  This means, for instance,
that I/O operations will typically require only constant memory (your machine should never run out of memory even when
processing TeraBytes of data).  Also, HadoopIO allows you to process data in a functional programming style, which e.g.
means via commonly used methods such as `map`, `filter`, or `foreach`.

That being said, HadoopIO was not intended for large-scale, distributed processing of HDFS data.  Currently all
operations will translate into a single-threaded, record-by-record I/O operation behind the scenes.  If you need to
increase the level of parallelism, then you must do so on your own -- e.g. via actors or multiple threads.


<a name="development"></a>

# Development

## Build requirements

* Java 7, preferably Oracle JDK 1.7


## Building the code

    $ ./sbt compile


## Running the tests

Run the test suite:

    $ ./sbt test


<a name="changelog"></a>

# Change log

See [CHANGELOG](CHANGELOG.md).


<a name="Contributing"></a>

# Contributing to this project

Code contributions, bug reports, feature requests etc. are all welcome.

If you are new to GitHub please read [Contributing to a project](https://help.github.com/articles/fork-a-repo) for how
to send patches and pull requests to this project


<a name="Authors"></a>

# Authors

* [Michael Noll](https://github.com/miguno)
* [Kevin Mao](https://github.com/KevinJMao)


<a name="License"></a>

# License

Copyright © 2015 [VeriSign, Inc.](http://www.verisigninc.com/)

See [LICENSE](LICENSE) for licensing information.


<a name="References"></a>

# References

Related projects:

* [hio](https://github.com/verisign/hio) -- command line utilities to interact with Hadoop HDFS.

Hadoop API

* [FileSystem](https://hadoop.apache.org/docs/current/api/org/apache/hadoop/fs/FileSystem.html)
* [FileUtil](https://hadoop.apache.org/docs/current/api/org/apache/hadoop/fs/FileUtil.html)
