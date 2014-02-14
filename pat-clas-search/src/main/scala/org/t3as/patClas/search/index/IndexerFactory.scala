/*
    Copyright 2013 NICTA
    
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

package org.t3as.patClas.search.index

import java.io.{Closeable, File}

import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.document.{FieldType, StringField}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.index.{FieldInfo, IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode.{CREATE, CREATE_OR_APPEND}
import org.apache.lucene.store.{Directory, FSDirectory}
import org.slf4j.LoggerFactory

import org.t3as.patClas.common.CPCTypes.ClassificationItem
import org.t3as.patClas.common.IPCTypes.IPCEntry
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.USPCTypes.UsClass
import org.t3as.patClas.common.Util
import org.t3as.patClas.search.Constants

trait Indexer[T] extends Closeable {
  def add(c: T): Unit
  def addTree(t: TreeNode[T]): Unit
}

object IndexerFactory {

  def getCPCIndexer(indexDir: File): Indexer[ClassificationItem] =
    new IndexerImpl[ClassificationItem](indexDir, cpcToDoc)

  def getIPCIndexer(indexDir: File): Indexer[IPCEntry] =
    new IndexerImpl[IPCEntry](indexDir, ipcToDoc)
  
  def getUSPCIndexer(indexDir: File): Indexer[UsClass] =
    new IndexerImpl[UsClass](indexDir, uspcToDoc)
    
  // for unit tests
  private[search] def getCPCTestIndexer(dir: Directory): Indexer[ClassificationItem] =
    new IndexerImpl[ClassificationItem](new File("not.used"), cpcToDoc) {
    override protected def open = new IndexWriter(dir, indexWriterConfig)
  }
  
  val highlightFieldType = {
    val t = new FieldType()
    t.setIndexed(true)
    t.setTokenized(true)
    // _AND_OFFSETS needed for PostingsHighlighter, not included by default
    t.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS)
    t.setStored(true) // TODO: I think field must be stored, but if not remove
    t.freeze()
    t
  }
  def highlightTextField(name: String, value: String) = new Field(name, value, highlightFieldType)

  private def cpcToDoc(c: ClassificationItem) = {
    import org.t3as.patClas.common.CPCTypes.IndexFieldName._
    
    val doc = new Document
    import org.t3as.patClas.common.CPCTypes.IndexFieldName._
    doc add new StringField(Symbol, c.symbol, Store.YES)
    doc add new StringField(Level, c.level.toString, Store.YES)

    if (!c.classTitle.isEmpty()) doc add highlightTextField(ClassTitle, Util.toText(c.classTitle))
    if (!c.notesAndWarnings.isEmpty()) doc add highlightTextField(NotesAndWarnings, Util.toText(c.notesAndWarnings))
    doc
  }

  private def ipcToDoc(c: IPCEntry) = {
    import org.t3as.patClas.common.IPCTypes.IndexFieldName._

    val doc = new Document
    import org.t3as.patClas.common.IPCTypes.IndexFieldName._
    doc add new StringField(Symbol, c.symbol, Store.YES)
    doc add new StringField(Level, c.level.toString, Store.YES)
    doc add new StringField(Kind, c.kind.toString, Store.YES)

    if (!c.textBody.isEmpty()) doc add highlightTextField(TextBody, Util.toText(c.textBody))
    doc
  }

  private def uspcToDoc(c: UsClass) = {
    import org.t3as.patClas.common.USPCTypes.IndexFieldName._

    val doc = new Document
    import org.t3as.patClas.common.USPCTypes.IndexFieldName._
    doc add new StringField(Symbol, c.symbol, Store.YES)
    Seq(
        (c.classTitle, ClassTitle, (s: String) => s), 
        (c.subClassTitle, SubClassTitle, Util.toText _), 
        (c.subClassDescription, SubClassDescription, Util.toText _),
        (Some(c.text), Text, Util.toText _)
    ) foreach {
      case(Some(text), field, format) => if (!text.isEmpty) doc add highlightTextField(field, format(text))
      case _ =>
    }
    doc
  }
  // case class UsClass(id: Option[Int], xmlId: String, parentXmlId: String, symbol: String, classTitle: Option[String], subClassTitle: Option[String], subClassDescription: Option[String], text: String) {
  // val ID, Symbol, ClassTitle, SubClassTitle, SubClassDescription, Text = Value
}

private class IndexerImpl[T](indexDir: File, mkDoc: T => Document) extends Indexer[T] {
  val log = LoggerFactory.getLogger(getClass)
  val writer = open

  protected def open = new IndexWriter(FSDirectory.open(indexDir), indexWriterConfig)

  protected def indexWriterConfig = {
    // This analyzer is used with TextFields, but not with StringFields.
    // It uses the following Lucene components:
    // StandardTokenizer, StandardFilter, EnglishPossessiveFilter, LowerCaseFilter, StopFilter, PorterStemFilter.
    val c = new IndexWriterConfig(Constants.version, Constants.analyzer)
    c.setOpenMode(CREATE)
    c
  }

  def add(t: T) = writer.addDocument(mkDoc(t))

  def addTree(t: TreeNode[T]): Unit = {
    add(t.value)
    t.children foreach { t => addTree(t) }
  }

  def close = {
    log.debug("numDocs = {}", writer.numDocs())
    writer.close
  }
}
