package com.verisign.hadoopio.avro

import com.miguno.avro.Tweet
import com.verisign.hadoopio.testing.Testing
import org.scalatest.{FunSpec, GivenWhenThen, Matchers}

import scala.language.reflectiveCalls

class AvroIteratorSpec extends FunSpec with Matchers with GivenWhenThen {

  describe("AvroIterator (when operating on local files)") {

    it("should iterate through a local file") {
      Given("some tweets")
      val tweets = Testing.fixture.tweets
      And("an Avro file that contains those tweets")
      val avroFile = Testing.fixture.tweetsAvroFile

      When("I create an iterator for the file")
      val itr: Iterator[Tweet] = AvroIterator.specificFrom[Tweet](avroFile)

      Then("the tweets exposed by the iterator match the original tweets")
      itr.toSeq should be(tweets)
    }

  }

}