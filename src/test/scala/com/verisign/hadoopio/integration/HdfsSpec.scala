package com.verisign.hadoopio.integration

import java.io.File
import java.nio.file.Paths

import com.verisign.hadoopio.logging.LazyLogging
import com.verisign.hadoopio.testing.Testing
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.hadoop.hdfs.{DFSConfigKeys, HdfsConfiguration, MiniDFSCluster}
import org.scalatest._

/**
 * A base test spec for tests that need to talk to HDFS.
 */
@DoNotDiscover
class HdfsSpec extends FunSpec with Matchers with GivenWhenThen with BeforeAndAfterAll with LazyLogging {

  private val MiniDfsBaseDir = Paths.get(Testing.TempDirectory, "hdfs").toString
  private val NameNodeEditsDir = Paths.get(Testing.TempDirectory, "nn-edits").toString
  private val DataNodeDataDir = Paths.get(Testing.TempDirectory, "dn-data").toString

  protected val clusterConfiguration: Configuration = {
    val conf = new HdfsConfiguration
    logger.debug(s"Using local directory $MiniDfsBaseDir as HDFS base directory for MiniDFSCluster")
    conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, MiniDfsBaseDir)
    logger.debug(s"Using local directory $NameNodeEditsDir as edits directory for NameNode")
    conf.set(DFSConfigKeys.DFS_NAMENODE_EDITS_DIR_KEY, NameNodeEditsDir)
    logger.debug(s"Using local directory $DataNodeDataDir as data directory for DataNode")
    conf.set(DFSConfigKeys.DFS_DATANODE_DATA_DIR_KEY, DataNodeDataDir)
    conf
  }

  protected var dfsCluster: MiniDFSCluster = null
  implicit protected var fs: FileSystem = null

  override def beforeAll() {
    if (dfsCluster == null) {
      dfsCluster = new MiniDFSCluster.Builder(clusterConfiguration).numDataNodes(2).build()
      dfsCluster.waitClusterUp()
      dfsCluster.waitActive()
      fs = dfsCluster.getFileSystem
    }
  }

  override def afterAll() {
    if (dfsCluster != null) {
      dfsCluster.shutdown()
      dfsCluster = null
      fs = null
      FileUtil.fullyDelete(new File(Testing.TempDirectory))
      ()
    }
  }

  describe("HdfsSpec") {

    it("should provide access to an in-memory HDFS cluster") {
      Testing.createPath(new Path("/hdfs/foo"))
    }

  }

}