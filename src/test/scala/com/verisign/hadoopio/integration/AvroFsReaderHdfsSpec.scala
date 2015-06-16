package com.verisign.hadoopio.integration

import com.miguno.avro.{Book, Tweet}
import com.verisign.hadoopio.avro.AvroFsReader
import com.verisign.hadoopio.testing.{CustomMatchers, Testing}
import CustomMatchers._
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.scalatest.BeforeAndAfterEach

import scala.language.reflectiveCalls
import scala.util.Try

/**
 * Integration tests for [[AvroFsReader]] that talk to HDFS.
 */
class AvroFsReaderHdfsSpec extends HdfsSpec with BeforeAndAfterEach {

  private var reader: AvroFsReader = null
  private val defaultDstDir = new Path("testing/AvroFsUtils/input")

  override protected def beforeEach() {
    reader = AvroFsReader(clusterConfiguration)
  }

  override protected def afterEach() {
    Try(Testing.deletePath(defaultDstDir))
  }

  describe("AvroFsUtils") {

    describe("when using the generic API of Avro") {

      it("should iterate through an Avro file") {
        Given("some records")
        val records = Testing.fixture.tweets
        And("an Avro file that contains those records")
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsAvroResource, defaultDstDir)

        When("I create an iterator for the file")
        val itr: Iterator[GenericRecord] = reader.read(dstFile.get)

        Then("the records exposed by the iterator match the original tweets")
        itr.toSeq should beSchemaEqual(records)
      }

      it("should iterate through an directory containing a single Avro file") {
        Given("some records")
        val records = Testing.fixture.tweets
        And("a single Avro file in a directory that contains those records")
        val dstDir = Testing.fixture.defaultDstDir
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, dstDir)

        When("I create an iterator for the directory")
        val itr: Iterator[GenericRecord] = reader.read(dstDir)

        Then("the records exposed by the iterator match the original tweets")
        itr.toSeq should beSchemaEqual(records)
      }

      it("should return an empty iterator when the Avro file contains no records") {
        Given("an Avro file that contains no records")
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsEmptyAvroResource, defaultDstDir)

        When("I create an iterator for the file")
        val itr: Iterator[GenericRecord] = reader.read(dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

      it("should return an empty iterator when a directory contains no files") {
        Given("a directory that contains only a sub-directory")
        val dirWithSubdirOnly = new Path("testing/AvroFsUtils/generic/dir-with-subdir-only")
        Testing.mkdirs(dirWithSubdirOnly)
        val subDir = new Path(dirWithSubdirOnly.toString, "foobar")
        Testing.mkdirs(subDir)

        When("I create an iterator for the directory")
        val itr: Iterator[GenericRecord] = reader.read(dirWithSubdirOnly)

        Then("the iterator is empty")
        itr should be('empty)

        // Cleanup
        Testing.deletePath(dirWithSubdirOnly)
      }

      it("should concatenate multiple files (with the same schema) in a directory according to the lexicographical " +
          "order of their names") {
        val firstFileName = "0001_0"
        val secondFileName = "0002_0"
        Given(s"two Avro files $firstFileName and $secondFileName with matching schemas")
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, firstFileName),
          destinationIsDirectory)
        Testing.uploadResource(Testing.fixture.tweets2AvroResource, new Path(defaultDstDir, secondFileName),
          destinationIsDirectory)

        When("I create an iterator for the directory")
        val itr: Iterator[GenericRecord] = reader.read(defaultDstDir)

        Then(s"the iterator contains the records of $firstFileName followed by the records of $secondFileName")
        val records = Testing.fixture.tweets ++ Testing.fixture.tweets2
        itr.toSeq should beSchemaEqual(records)
      }

      it("should concatenate multiple files (with different schemas) in a directory according to the lexicographical " +
          "order of their names") {
        val firstFileName = "A"
        val secondFileName = "B"
        Given(s"two Avro files $firstFileName and $secondFileName with different schemas")
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.booksAvroResource, new Path(defaultDstDir, firstFileName),
          destinationIsDirectory)
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, secondFileName),
          destinationIsDirectory)

        When("I create an iterator for the directory")
        val itr: Iterator[GenericRecord] = reader.read(defaultDstDir)

        Then(s"the iterator contains the records of $firstFileName followed by the records of $secondFileName")
        val records = Testing.fixture.books ++ Testing.fixture.tweets
        itr.toSeq should beSchemaEqual(records)
      }

      it("should only read files that match the given include pattern") {
        Given("an include pattern")
        val includePattern = """0001_.*""".r
        And(s"an Avro file whose name matches the pattern")
        val matchingFileName = "0001_0"
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, matchingFileName),
          destinationIsDirectory)

        And(s"an Avro file whose name does not match the pattern")
        val nonMatchingFileName = "0002_0"
        Testing.uploadResource(Testing.fixture.tweets2AvroResource, new Path(defaultDstDir, nonMatchingFileName),
          destinationIsDirectory)

        When("I create an iterator for the directory")
        val itr: Iterator[GenericRecord] = reader.read(defaultDstDir, includePattern)

        Then("the iterator contains only the records of the matching file")
        val records = Testing.fixture.tweets
        itr.toSeq should beSchemaEqual(records)
      }

      it("should return an empty iterator for corrupt Avro files") {
        Given("a corrupt Avro file")
        val dstDir = Testing.fixture.defaultDstDir
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsCorruptAvroResource, dstDir)

        When("I create an iterator for the file")
        val itr: Iterator[GenericRecord] = reader.read(dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

      it("should return an empty iterator for non-Avro files") {
        Given("a text file")
        val dstDir = Testing.fixture.defaultDstDir
        val dstFile = Testing.uploadResource(Testing.fixture.textResource, dstDir)

        When("I create an iterator for the file")
        val itr: Iterator[GenericRecord] = reader.read(dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

    }

    describe("when using the specific API of Avro") {

      it("should iterate through an Avro file") {
        Given("some tweets")
        val tweets = Testing.fixture.tweets
        And("an Avro file that contains those tweets")
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsAvroResource, defaultDstDir)

        When("I create an iterator for the file")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](dstFile.get)

        Then("the tweets exposed by the iterator match the original tweets")
        itr.toSeq should be(tweets)
      }

      it("should iterate through a directory containing a single Avro file") {
        Given("some tweets")
        val tweets = Testing.fixture.tweets
        And("a single Avro file in a directory that contains those tweets")
        val dstDir = Testing.fixture.defaultDstDir
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, dstDir)

        When("I create an iterator for the directory")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](dstDir)

        Then("the tweets exposed by the iterator match the original tweets")
        itr.toSeq should be(tweets)
      }

      it("should return an empty iterator when the Avro file contains no records") {
        Given("an Avro file that contains no records")
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsEmptyAvroResource, defaultDstDir)

        When("I create an iterator for the file")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

      it("should return an empty iterator when a directory contains no files") {
        Given("a directory that contains only a sub-directory")
        val dirWithSubdirOnly = new Path("testing/AvroFsUtils/specific/dir-with-subdir-only")
        Testing.mkdirs(dirWithSubdirOnly)
        val subDir = new Path(dirWithSubdirOnly.toString, "foobar")
        Testing.mkdirs(subDir)

        When("I create an iterator for the directory")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](dirWithSubdirOnly)

        Then("the iterator is empty")
        itr should be('empty)

        // Cleanup
        Testing.deletePath(dirWithSubdirOnly)
      }

      it("should concatenate multiple files (with the same schema) in a directory according to the lexicographical " +
          "order of their names") {
        val firstFileName = "0001_0"
        val secondFileName = "0002_0"
        Given(s"two Avro files $firstFileName and $secondFileName with matching schemas")
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, firstFileName),
          destinationIsDirectory)
        Testing.uploadResource(Testing.fixture.tweets2AvroResource, new Path(defaultDstDir, secondFileName),
          destinationIsDirectory)

        When("I create an iterator for the directory")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](defaultDstDir)

        Then(s"the iterator contains the tweets of $firstFileName followed by the tweets of $secondFileName")
        val tweets = Testing.fixture.tweets ++ Testing.fixture.tweets2
        itr.toSeq should be(tweets)
      }

      // TODO: Define semantics when multiple files (with different schemas) are being read from a directory.
      // Currently, the behavior is that the returned iterator looks as if its of the requested type, but in fact it may
      // contain records of different types, too, leading to run-time failures!
      it("should define the semantics when multiple files (with different schemas) in a directory are being read")(pending)

      /**
       * Why does this "failure" happen?
       *
       * As of version Avro 1.7.7, the `D next(...)` of [[org.apache.avro.file.DataFileStream]] will perform a run-time
       * cast of the read record to the desired type `D` -- which may fail at run-time of course.  Unfortunately from
       * a user perspective the iterator returned by `iterator()` of  [[org.apache.avro.file.DataFileStream]] (which we
       * use in [[com.verisign.hadoopio.avro.AvroIterator]]) will still "look" type-safe.
       *
       * If we want to make our iterators more type-safe, we could e.g. silently discard any [[ClassCastException]]s
       * when our `next()` is called, though this may mean discarding possibly millions of records depending on the file
       * size -- even though we know that if the first record of a file is of the wrong type, then most likely the
       * remaining records will be of the wrong type, too.
       * A better, more scalable approach would be to test whether the first record in a file has the correct type,
       * and then assume the remaining records are correct, too, which should be a safe assumption in our context.
       * Unfortunately this would make the current code more complex, and we would also need to open a file handle /
       * network connection to HDFS just to achieve safe+scalable iterators.
       * Maybe we can come up with even better approaches with some thinking, although we may also decide to just
       * live with the current limitation and clarify the contract of the API in the case of the Avro-specific
       * functionality.
       */
      it("should fail in certain conditions when reading files with different schemas using a typed iterator") {
        val firstFileName = "A"
        val secondFileName = "B"
        Given(s"two Avro files $firstFileName and $secondFileName with different schemas")
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.booksAvroResource, new Path(defaultDstDir, firstFileName),
          destinationIsDirectory)
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, secondFileName),
          destinationIsDirectory)

        When(s"I create a typed iterator for the directory according to the schema of $firstFileName")
        val itrBooks: Iterator[Book] = reader.readSpecific[Book](defaultDstDir)

        Then(s"the iterator includes records of $secondFileName's type")
        And("accessing those records trigger ClassCastExceptions at run-time")
        val seeminglyBooksOnly: Seq[Book] = itrBooks.toSeq
        // Given the test data, all but the first 3 elements in seeminglyBooksOnly are actually of type `Tweet`.
        a[java.lang.ClassCastException] should be thrownBy seeminglyBooksOnly(3)
        a[java.lang.ClassCastException] should be thrownBy seeminglyBooksOnly(4).getAuthor

        When(s"I create a typed iterator for the directory according to the schema of $secondFileName")
        val itrTweets: Iterator[Tweet] = reader.readSpecific[Tweet](defaultDstDir)

        Then(s"the iterator includes records of $firstFileName's type")
        And("accessing those records trigger ClassCastExceptions at run-time")
        val seeminglyTweetsOnly: Seq[Tweet] = itrTweets.toSeq
        // Given the test data, the first 3 elements in seeminglyTweetsOnly are actually of type `Book`.
        a[java.lang.ClassCastException] should be thrownBy seeminglyTweetsOnly(0)
        a[java.lang.ClassCastException] should be thrownBy seeminglyTweetsOnly(1).getTimestamp
      }

      it("should only read files that match the given include pattern") {
        Given("an include pattern")
        val includePattern = """0001_.*""".r
        And(s"an Avro file whose name matches the pattern")
        val matchingFileName = "0001_0"
        val destinationIsDirectory = false
        Testing.uploadResource(Testing.fixture.tweetsAvroResource, new Path(defaultDstDir, matchingFileName),
          destinationIsDirectory)

        And(s"an Avro file whose name does not match the pattern")
        val nonMatchingFileName = "0002_0"
        Testing.uploadResource(Testing.fixture.tweets2AvroResource, new Path(defaultDstDir, nonMatchingFileName),
          destinationIsDirectory)

        When("I create an iterator for the directory")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](defaultDstDir, includePattern)

        Then("the iterator contains only the tweets of the matching file")
        val tweets = Testing.fixture.tweets
        itr.toSeq should be(tweets)
      }

      it("should return an empty iterator for corrupt Avro files") {
        Given("a corrupt Avro file")
        val dstDir = Testing.fixture.defaultDstDir
        val dstFile = Testing.uploadResource(Testing.fixture.tweetsCorruptAvroResource, dstDir)

        When("I create an iterator for the file")
        val itr: Iterator[Tweet] = reader.readSpecific[Tweet](dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

      it("should return an empty iterator for non-Avro files") {
        Given("a text file")
        val dstDir = Testing.fixture.defaultDstDir
        val dstFile = Testing.uploadResource(Testing.fixture.textResource, dstDir)

        When("I create an iterator for the file")
        val itr: Iterator[GenericRecord] = reader.readSpecific[Tweet](dstFile.get)

        Then("the iterator is empty")
        itr should be('empty)
      }

    }

  }

}
