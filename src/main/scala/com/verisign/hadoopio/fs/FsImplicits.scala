package com.verisign.hadoopio.fs

import org.apache.hadoop.fs.RemoteIterator

/**
 * Improves idiomatic use of [[org.apache.hadoop.fs]] classes in Scala.
 */
object FsImplicits {

  private class WrappedRemoteIterator[T](private val itr: RemoteIterator[T]) extends java.util.Iterator[T] {

    override def hasNext: Boolean = itr.hasNext

    override def next(): T = itr.next()

    override def remove(): Unit = throw new UnsupportedOperationException

  }

  implicit class RemoteIteratorExtensions[T](val itr: RemoteIterator[T]) extends AnyVal {

    import scala.collection.JavaConverters.asScalaIteratorConverter

    def asScala: Iterator[T] = new WrappedRemoteIterator[T](itr).asScala

  }

}