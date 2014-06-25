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

package org.t3as.patClas.parse

import java.io.{Closeable, File}

import org.apache.lucene.document.{Document, Field, SortedDocValuesField}
import org.apache.lucene.index.{DirectoryReader, SlowCompositeReaderWrapper, TermsEnum}
import org.apache.lucene.search.suggest.InputIterator
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.BytesRef
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.{CPCUtil, IPCUtil, USPCUtil}
import org.t3as.patClas.common.Util.toText
import org.t3as.patClas.common.search.{Constants, Indexer}
import org.t3as.patClas.common.search.Indexer.{keywordFieldType, textFieldType, textFieldUnstemmedType}

object IndexerFactory {
  val log = LoggerFactory.getLogger(getClass)

  def inIter(dr: DirectoryReader, fields: List[String]) = new InputIterator with Closeable {
    val r = SlowCompositeReaderWrapper.wrap(dr)
    // iterator over all terms in all specified fields
    // TODO: subsequent fields may repeat terms in previous fields, so duplicates are likely. Is it a problem?
    // When the suggester fetches a term with a duplicate, what weight does it use?
    val termIter = fields.map { f =>
      var te: TermsEnum = null
      te = r.terms(f).iterator(te) // mutable
      Iterator.continually(te.next).takeWhile(_ != null).map((_, te.docFreq))
    }.foldLeft(Iterator.empty: Iterator[(BytesRef, Int)]) { _ ++ _ }

    override def getComparator = null

    var w: Int = 0
    override def next = if (termIter.hasNext) {
      val x = termIter.next
      w = x._2
      x._1
    } else null

    override def weight = w

    // payloads can be used to store info associated with the term
    override def hasPayloads = false
    override def payload = null

    // contexts can be used to filter suggestions
    override def hasContexts() = false
    override def contexts() = null

    override def close = r.close // closes dr, but SlowCompositeReaderWrapper has a TODO comment saying that maybe it shouldn't
  }

  def addText(doc: Document, field: String, unstemmedField: String, text: String) {
    doc add new Field(field, text, textFieldType)
    doc add new Field(unstemmedField, text, textFieldUnstemmedType)
  }

  def cpcToDoc(n: CPCParser.CPCNode) = {
    import org.t3as.patClas.common.CPCUtil.IndexFieldName._

    val c = n.classificationItem
    val doc = new Document
    
    doc add new Field(Symbol, c.symbol.toLowerCase, keywordFieldType)
    doc add new Field(Level, c.level.toString, keywordFieldType)
    if (!c.classTitle.isEmpty) addText(doc, ClassTitle, ClassTitleUnstemmed, toText(c.classTitle))
    if (!c.notesAndWarnings.isEmpty) addText(doc, NotesAndWarnings, NotesAndWarningsUnstemmed, toText(c.classTitle))
    addText(doc, HText, HTextUnstemmed, n.hText)
    doc
  }

  def getCPCIndexer(indexDir: File) = new Indexer(Constants.cpcAnalyzer, FSDirectory.open(indexDir), cpcToDoc)

  def getCPCSuggestionsSource(indexDir: File) = {
    import org.t3as.patClas.common.CPCUtil.unstemmedTextFields
    inIter(DirectoryReader.open(FSDirectory.open(indexDir)), unstemmedTextFields)
  }

  def ipcToDoc(n: IPCParser.IPCNode) = {
    import org.t3as.patClas.common.IPCUtil.toCpcFormat
    import org.t3as.patClas.common.IPCUtil.IndexFieldName._

    val c = n.ipcEntry
    val doc = new Document
    doc add new Field(Symbol, toCpcFormat(c.symbol).toLowerCase, keywordFieldType)
    doc add new Field(SymbolRaw, c.symbol, keywordFieldType)
    doc add new Field(Level, c.level.toString, keywordFieldType)
    doc add new Field(Kind, c.kind.toString, keywordFieldType)
    if (!c.textBody.isEmpty) addText(doc, TextBody, TextBodyUnstemmed, toText(c.textBody))
    addText(doc, HText, HTextUnstemmed, n.hText)
    doc
  }

  def getIPCIndexer(indexDir: File) = new Indexer(Constants.ipcAnalyzer, FSDirectory.open(indexDir), ipcToDoc)

  def getIPCSuggestionsSource(indexDir: File) = {
    import org.t3as.patClas.common.IPCUtil.unstemmedTextFields
    inIter(DirectoryReader.open(FSDirectory.open(indexDir)), unstemmedTextFields)
  }

  def uspcToDoc(c: USPCUtil.UsClass) = {
    import org.t3as.patClas.common.USPCUtil.IndexFieldName._

    val doc = new Document
    doc add new Field(Symbol, c.symbol.toLowerCase, keywordFieldType)

    Seq(
      (c.classTitle, ClassTitle, ClassTitleUnstemmed, (s: String) => s),
      (c.subClassTitle, SubClassTitle, SubClassTitleUnstemmed, toText _),
      (c.subClassDescription, SubClassDescription, SubClassDescriptionUnstemmed, toText _),
      (Some(c.text), Text, TextUnstemmed, toText _)) foreach {
        case (Some(text), field, fieldUnstemmed, format) if !text.isEmpty => addText(doc, field, fieldUnstemmed, format(text))
        case _ =>
      }

    doc
  }

  def getUSPCIndexer(indexDir: File) = new Indexer(Constants.uspcAnalyzer, FSDirectory.open(indexDir), uspcToDoc)

  def getUSPCSuggestionsSource(indexDir: File) = {
    import org.t3as.patClas.common.USPCUtil.unstemmedTextFields
    inIter(DirectoryReader.open(FSDirectory.open(indexDir)), unstemmedTextFields)
  }

}

