package com.verisign.hadoopio.avro

import com.verisign.hadoopio.fs.FsUtils
import AvroImplicits._
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecordBase
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.util.matching.Regex

class AvroFsReader(cfg: Configuration) {

  private implicit val impCfg = cfg

  def read(path: Path, include: Regex = ".*".r): Iterator[GenericRecord] = {
    implicit val impFs = path.getFileSystem(cfg)
    val files = FsUtils.files(path, include)
    files.map { f => f.toAvro.toOption}.flatten.foldLeft(Iterator[GenericRecord]())(_ ++ _)
  }

  def readSpecific[S <: SpecificRecordBase](path: Path, include: Regex = ".*".r): Iterator[S] = {
    implicit val impFs = path.getFileSystem(cfg)
    val files = FsUtils.files(path, include)
    files.map { f => f.toSpecificAvro[S].toOption}.flatten.foldLeft(Iterator[S]())(_ ++ _)
  }

}

object AvroFsReader {

  def apply(cfg: Configuration) = new AvroFsReader(cfg)

}