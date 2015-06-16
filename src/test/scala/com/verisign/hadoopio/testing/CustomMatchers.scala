package com.verisign.hadoopio.testing

import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.specific.SpecificRecordBase
import org.scalatest.matchers.{MatchResult, Matcher}

trait CustomMatchers {

  private class AvroGenericRecordMatcher(record: GenericRecord) extends Matcher[GenericRecord] {

    private def avroGenericEqual(actual: GenericRecord, expected: GenericRecord): Boolean =
      GenericData.get().compare(actual, expected, expected.getSchema) == 0

    def apply(actualRecord: GenericRecord): MatchResult = {
      MatchResult(
        avroGenericEqual(actualRecord, record),
        s"$actualRecord was not semantically equal to $record",
        s"$actualRecord was semantically equal to $record")
    }

  }

  /**
   * True if the record is equal to `expectedRecord` according to the schema of `expectedRecord`.
   *
   * Should be used when testing [[GenericRecord]], i.e. Avro's generic API.
   *
   * @param expectedRecord The expected record.
   * @return True if equal, false otherwise.
   */
  def beSchemaEqual(expectedRecord: GenericRecord): Matcher[GenericRecord] =
    new AvroGenericRecordMatcher(expectedRecord)

  private class SeqAvroGenericRecordMatcher(records: Seq[GenericRecord]) extends Matcher[Seq[GenericRecord]] {

    def apply(actualRecords: Seq[GenericRecord]): MatchResult = {
      val sameSize = actualRecords.size == records.size
      val sameContent = actualRecords.zip(records) map { case (a, e) => beSchemaEqual(e)(a)} forall (m => m.matches)
      MatchResult(
        sameSize && sameContent,
        s"$actualRecords was not semantically equal to $records",
        s"$actualRecords was semantically equal to $records")
    }

  }

  /**
   * True if the records are equal to `expectedRecords` according to the schema of `expectedRecords`.
   *
   * Should be used when testing [[GenericRecord]], i.e. Avro's generic API.
   *
   * @param expectedRecords The expected records.
   * @return True if equal, false otherwise.
   */
  def beSchemaEqual(expectedRecords: Seq[GenericRecord]): Matcher[Seq[GenericRecord]] =
    new SeqAvroGenericRecordMatcher(expectedRecords)

  private class AvroSpecificRecordMatcher[S <: SpecificRecordBase](record: S) extends Matcher[S] {

    private def avroSpecificEqual(actual: S, expected: S): Boolean =
      actual.compareTo(expected) == 0

    def apply(actualRecord: S): MatchResult = {
      MatchResult(
        avroSpecificEqual(actualRecord, record),
        s"$actualRecord was not semantically equal to $record",
        s"$actualRecord was semantically equal to $record")
    }

  }

  /**
   * True if the record is equal to `expectedRecord` according to the schema of `expectedRecord`.
   *
   * Should be used when testing [[SpecificRecordBase]], i.e. Avro's specific API.
   *
   * @param expectedRecord The expected record.
   * @return True if equal, false otherwise.
   */
  def beSpecificSchemaEqual[S <: SpecificRecordBase](expectedRecord: S): Matcher[S] =
    new AvroSpecificRecordMatcher[S](expectedRecord)

  private class SeqAvroSpecificRecordMatcher[S <: SpecificRecordBase](records: Seq[S]) extends Matcher[Seq[S]] {

    def apply(actualRecords: Seq[S]): MatchResult = {
      val sameSize = actualRecords.size == records.size
      val sameContent = if (actualRecords.nonEmpty) {
        val schema = actualRecords(0).getSchema
        actualRecords.zip(records) map { case (a, e) => beSpecificSchemaEqual(e)(a)} forall (m => m.matches)
      }
      else true

      //val sameContent = actualRecords.zip(records) map { case (a, e) => beSpecificSchemaEqual(e)(a)} forall (m => m.matches)
      MatchResult(
        sameSize && sameContent,
        s"$actualRecords was not semantically equal to $records",
        s"$actualRecords was semantically equal to $records")
    }

  }

  /**
   * True if the records are equal to `expectedRecords` according to the schema of `expectedRecords`.
   *
   * Should be used when testing [[SpecificRecordBase]], i.e. Avro's specific API.
   *
   * @param expectedRecords The expected records.
   * @return True if equal, false otherwise.
   */
  def beSpecificSchemaEqual[S <: SpecificRecordBase](expectedRecords: Seq[S]): Matcher[Seq[S]] =
    new SeqAvroSpecificRecordMatcher[S](expectedRecords)

}

/**
 * Make the custom matchers easy to import with: `import CustomMatchers._`
 */
object CustomMatchers extends CustomMatchers