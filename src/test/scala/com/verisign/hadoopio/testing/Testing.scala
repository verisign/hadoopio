package com.verisign.hadoopio.testing

import java.io.File
import java.nio.file.Paths

import com.google.common.io.Resources
import com.miguno.avro.{Book, Tweet}
import com.verisign.hadoopio.logging.LazyLogging
import org.apache.hadoop.fs.{FileSystem, Path}

import scala.util.Try

object Testing extends LazyLogging {

  private val KeepSrcFile = false
  private val OverwriteExistingDstFile = true

  def fixture = new {

    val tweetsEmptyAvroResource = "avro/tweets-empty.avro"
    val tweetsEmptyAvroFile = new File(fileNameOf(tweetsEmptyAvroResource))

    // These tweets must match the contents of `tweetsAvroResource`.
    val tweets = Seq(
      new Tweet("miguno", "Rock: Nerf paper, scissors is fine.", 1366150681L),
      new Tweet("BlizzardCS", "Works as intended.  Terran is IMBA.", 1366154481L),
      new Tweet("DarkTemplar", "From the shadows I come!", 1366154681L),
      new Tweet("VoidRay", "Prismatic core online!", 1366160000L),
      new Tweet("VoidRay", "Fire at will, commander.", 1366160010L),
      new Tweet("DarkTemplar", "I am the blade of Shakuras!", 1366174681L),
      new Tweet("Immortal", "I return to serve!", 1366175681L),
      new Tweet("Immortal", "En Taro Adun!", 1366176283L),
      new Tweet("VoidRay", "There is no greater void than the one between your ears.", 1366176300L),
      new Tweet("DarkTemplar", "I strike from the shadows!", 1366184681L)
    )

    val tweetsAvroResource = "avro/tweets1.avro"
    val tweetsAvroFile = new File(fileNameOf(tweetsAvroResource))

    // A "corrupt" version of `tweetsAvroResource`, created via `head -c 390 tweets1.avro`.
    val tweetsCorruptAvroResource = "avro/tweets1-corrupt.avro"
    val tweetsCorruptAvroFile = new File(fileNameOf(tweetsCorruptAvroResource))

    // These tweets must match the contents of `tweets2AvroResource`.
    val tweets2 = Seq(
      new Tweet("Zergling", "Cthulhu R'lyeh!", 1366154399L),
      new Tweet("miguno", "4-Gate is the new 6-Pool.", 1366150900L),
      new Tweet("Stalker", "I am less stupid than Dragoons!", 1366177000L),
      new Tweet("DarkTemplar", "The void claims its own.", 1366184500L)
    )

    val tweets2AvroResource = "avro/tweets2.avro"
    val tweets2AvroFile = new File(fileNameOf(tweets2AvroResource))

    // A text file, i.e. a file that is not an Avro file.
    val textResource = "a-text-file.txt"
    val textFile = new File(fileNameOf(textResource))

    // These tweets must match the contents of `booksAvroResource`.
    val books = Seq(
      new Book("The CIA and the Cult of Intelligence", "Victor Marchetti", "978-0394482392"),
      new Book("Secrets and Lies: Digital Security in a Networked World", "Bruce Schneier", "978-0471253112"),
      new Book("The Denial of Death", "Ernest Becker", "978-0684832401")
    )

    val booksAvroResource = "avro/books.avro"
    val booksAvroFile = new File(fileNameOf(booksAvroResource))

    // Default target directory for placing test files into HDFS.
    val defaultDstDir = new Path("testing/tweets/input")

  }

  val TempDirectory = {
    val tmpDir = System.getProperty("java.io.tmpdir")
    Paths.get(tmpDir, "hadoopio", "mini-dfs-cluster").toString
  }

  def fileNameOf(resourceFile: String): String = Resources.getResource(resourceFile).getPath

  def createPath(path: Path)(implicit fs: FileSystem) {
    val out = fs.create(path)
    out.flush()
    out.close()
  }

  def mkdirs(path: Path)(implicit fs: FileSystem): Unit = {
    fs.mkdirs(path)
  }

  def deletePath(path: Path)(implicit fs: FileSystem) {
    val recursive = true
    fs.delete(path, recursive)
  }

  def uploadResource(resourceFile: String, destination: Path, destinationIsDirectory: Boolean = true)
                    (implicit fs: FileSystem): Try[Path] = {
    logger.debug("Uploading local file " + resourceFile + " to HDFS path " + destination)
    Try {
      val src = new Path(Testing.fileNameOf(resourceFile))
      val dst = if (destinationIsDirectory) new Path(destination, src.getName) else destination
      fs.copyFromLocalFile(KeepSrcFile, OverwriteExistingDstFile, src, dst)
      logger.debug("Successfully uploaded " + resourceFile + " to " + dst)
      dst
    }
  }

}