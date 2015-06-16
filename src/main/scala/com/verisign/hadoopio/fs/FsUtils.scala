package com.verisign.hadoopio.fs

import FsImplicits._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.util.matching.Regex

/**
 * Helper methods to interact with HDFS and similar supported file systems.
 */
object FsUtils {

  private val FLAG_NO_DIRECTORY_RECURSION = false

  private def allFiles(path: Path)(implicit cfg: Configuration): Seq[Path] = {
    val fs = path.getFileSystem(cfg)
    if (fs.isFile(path)) Stream(path)
    else if (fs.isDirectory(path)) {
      val nodes = fs.listFiles(path, FLAG_NO_DIRECTORY_RECURSION).asScala
      nodes.filter(_.isFile).map(_.getPath).toStream
    }
    else Stream.empty[Path]
  }

  /**
   * - If `path` is a file, then the returned sequence contains only `path`.
   * - If `path` is a directory, then we return the list of files in `path`.  We do not recurse into sub-directories.
   */
  def files(path: Path, include: Regex = ".*".r)(implicit cfg: Configuration) =
    allFiles(path).filter(f => include.pattern.matcher(f.getName).matches)

}