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

import java.io.Closeable

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.document.{Document, FieldType}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.FieldInfo.IndexOptions
import org.apache.lucene.index.FieldInfo.IndexOptions.{DOCS_AND_FREQS, DOCS_AND_FREQS_AND_POSITIONS, DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS}
import org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.TreeNode

object Indexer {

  private def mkFieldType(tokenized: Boolean, stored: Boolean, opt: IndexOptions) = {
    val t = new FieldType
    t.setIndexed(true)
    t.setTokenized(tokenized)
    t.setIndexOptions(opt)
    t.setStored(stored)
    t.freeze()
    t
  }
  
  val keywordFieldType = mkFieldType(false, true, DOCS_AND_FREQS)
  // Typical default to support phrase queries is DOCS_AND_FREQS_AND_POSITIONS
  // _AND_OFFSETS needed for PostingsHighlighter. Also needs stored field.
  val textFieldType = mkFieldType(true, true, DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
  // TODO: could I get highlighting to use the stemmed stored field and avoid storing the unstemmed field (it's the same data)
  val textFieldUnstemmedType = textFieldType
  
  val hTextType = mkFieldType(true, false, DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
  val hTextUnstemmedType = hTextType
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
