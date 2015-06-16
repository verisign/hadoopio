package com.verisign.hadoopio.avro

import java.io.File

import org.apache.avro.file.{DataFileReader, FileReader}
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.mapred.FsInput
import org.apache.avro.specific.{SpecificDatumReader, SpecificRecordBase}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

/**
 * Read an Avro file via a Scala [[Iterator]].
 */
object AvroIterator {

  import scala.collection.JavaConverters.asScalaIteratorConverter

  @deprecated("You should use specificFrom(file: Path)", since = "0.1.0")
  def specificFrom[S <: SpecificRecordBase](file: File): Iterator[S] = AvroJavaIterator.specificFrom[S](file).asScala

  def specificFrom[S <: SpecificRecordBase](file: Path)(implicit cfg: Configuration): Iterator[S] =
    AvroJavaIterator.specificFrom[S](file).asScala

  @deprecated("You should use genericFrom(file: Path)", since = "0.1.0")
  def genericFrom[G <: GenericRecord](file: File): Iterator[G] = AvroJavaIterator.genericFrom[G](file).asScala

  def genericFrom[G <: GenericRecord](file: Path)(implicit cfg: Configuration): Iterator[G] =
    AvroJavaIterator.genericFrom[G](file).asScala

}


/**
 * Read an Avro file via a Java [[java.util.Iterator]].
 */
object AvroJavaIterator {

  @deprecated("You should use specificFrom(file: Path)", since = "0.1.0")
  def specificFrom[S <: SpecificRecordBase](file: File): java.util.Iterator[S] =
    AvroFileReader.specificFrom[S](file).iterator()

  def specificFrom[S <: SpecificRecordBase](file: Path)(implicit cfg: Configuration): java.util.Iterator[S] =
    AvroFileReader.specificFrom[S](file).iterator()

  @deprecated("You should use genericFrom(file: Path)", since = "0.1.0")
  def genericFrom[G <: GenericRecord](file: File): java.util.Iterator[G] =
    AvroFileReader.genericFrom[G](file).iterator()

  def genericFrom[G <: GenericRecord](file: Path)(implicit cfg: Configuration): java.util.Iterator[G] =
    AvroFileReader.genericFrom[G](file).iterator()

}


/**
 * Read an Avro file via an Avro [[org.apache.avro.file.FileReader]], which provides the best low-level access to the
 * Avro file if that is what you are interested in.
 */
object AvroFileReader {

  @deprecated("You should use specificFrom(file: Path)", since = "0.1.0")
  def specificFrom[S <: SpecificRecordBase](file: File): FileReader[S] = {
    val reader = new SpecificDatumReader[S]
    DataFileReader.openReader[S](file, reader)
  }

  def specificFrom[S <: SpecificRecordBase](path: Path)(implicit cfg: Configuration): FileReader[S] = {
    val input = new FsInput(path, cfg)
    val reader = new SpecificDatumReader[S]
    DataFileReader.openReader[S](input, reader)
  }

  @deprecated("You should use genericFrom(file: Path)", since = "0.1.0")
  def genericFrom[G <: GenericRecord](file: File): FileReader[G] = {
    val reader = new GenericDatumReader[G]
    DataFileReader.openReader[G](file, reader)
  }

  def genericFrom[G <: GenericRecord](path: Path)(implicit cfg: Configuration): FileReader[G] = {
    val input = new FsInput(path, cfg)
    val reader = new GenericDatumReader[G]
    DataFileReader.openReader[G](input, reader)
  }

}