/*
    Copyright 2013, 2014 NICTA
    
    This file is part of t3as (Text Analysis As A Service).

    t3as is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    t3as is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with t3as.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.t3as.patClas.common.search

import java.io.File
import org.apache.lucene.document.Document
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.TreeNode
import java.io.Closeable
import org.apache.lucene.document.FieldType
import org.apache.lucene.document.Field
import org.apache.lucene.index.FieldInfo
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.store.Directory

object Indexer {

  private def mkFieldType(tokenized: Boolean) = {
    val t = new FieldType
    t.setIndexed(true)
    t.setTokenized(tokenized)
    // _AND_OFFSETS needed for PostingsHighlighter, not included by default
    t.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
    t.setStored(true) // TODO: I think field must be stored, but if not remove
    t.freeze()
    t
  }
  
  val keywordFieldType = mkFieldType(false)
  val textFieldType = mkFieldType(true)
}

class Indexer[T](analyzer: Analyzer, dir: Directory, mkDoc: T => Document) extends Closeable {
  val log = LoggerFactory.getLogger(getClass)

  val writer = new IndexWriter(dir, indexWriterConfig)

  protected def indexWriterConfig = {
    val c = new IndexWriterConfig(Constants.version, analyzer)
    c.setOpenMode(CREATE)
    c
  }

  def add(t: T) = writer.addDocument(mkDoc(t))

  def addTree(t: TreeNode[T]): Unit = {
    add(t.value)
    t.children foreach { t => addTree(t) }
  }

  override def close = {
    log.debug("numDocs = {}", writer.numDocs())
    writer.close
  }
}
