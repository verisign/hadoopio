package com.verisign.hadoopio.avro

import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.util.Try

/**
 * Adds Avro capabilities to [[org.apache.hadoop.fs]] classes.
 */
object AvroImplicits {

  implicit class PathExtensions(val path: Path) extends AnyVal {

    /**
     * Requirement: `path` must be an HDFS file in Avro format (and e.g. is not a directory containing such files).
     */
    def toAvro(implicit cfg: Configuration = new Configuration): Try[Iterator[GenericRecord]] =
      Try(AvroIterator.genericFrom[GenericRecord](path))

    /**
     * Requirement: `path` must be an HDFS file in Avro format (and e.g. is not a directory containing such files).
     */
    def toSpecificAvro[S <: SpecificRecordBase](implicit cfg: Configuration = new Configuration): Try[Iterator[S]] =
      Try(AvroIterator.specificFrom[S](path))

  }

}