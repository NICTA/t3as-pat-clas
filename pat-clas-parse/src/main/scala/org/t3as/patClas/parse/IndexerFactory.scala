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

package org.t3as.patClas.parse

import java.io.File
import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.StringField
import org.t3as.patClas.common.Util.toText
import org.t3as.patClas.common.search.Indexer
import org.t3as.patClas.common.search.Indexer.highlightFieldType
import org.t3as.patClas.api.CPC.ClassificationItem
import org.t3as.patClas.api.IPC.IPCEntry
import org.t3as.patClas.api.USPC.UsClass

object IndexerFactory {

  private def cpcToDoc(c: ClassificationItem) = {
    import org.t3as.patClas.api.CPC.IndexFieldName._
    
    val doc = new Document
    doc add new StringField(Symbol, c.symbol, Store.YES)
    doc add new StringField(Level, c.level.toString, Store.YES)

    if (!c.classTitle.isEmpty())
      doc add new Field(ClassTitle, toText(c.classTitle), highlightFieldType)

    if (!c.notesAndWarnings.isEmpty())
      doc add new Field(NotesAndWarnings, toText(c.notesAndWarnings), highlightFieldType)

    doc
  }

  def getCPCIndexer(indexDir: File) = new Indexer[ClassificationItem](indexDir, cpcToDoc)
  
  private def ipcToDoc(c: IPCEntry) = {
    import org.t3as.patClas.api.IPC.IndexFieldName._

    val doc = new Document
    doc add new StringField(Symbol, c.symbol, Store.YES)
    doc add new StringField(Level, c.level.toString, Store.YES)
    doc add new StringField(Kind, c.kind.toString, Store.YES)

    if (!c.textBody.isEmpty())
      doc.add(new Field(TextBody, toText(c.textBody), highlightFieldType))
      
    doc
  }

  def getIPCIndexer(indexDir: File) = new Indexer[IPCEntry](indexDir, ipcToDoc)
  
  private def uspcToDoc(c: UsClass) = {
    import org.t3as.patClas.api.USPC.IndexFieldName._

    val doc = new Document
    doc add new StringField(Symbol, c.symbol, Store.YES)
    
    Seq(
        (c.classTitle, ClassTitle, (s: String) => s), 
        (c.subClassTitle, SubClassTitle, toText _), 
        (c.subClassDescription, SubClassDescription, toText _),
        (Some(c.text), Text, toText _)
    ) foreach {
      case(Some(text), field, format) => if (!text.isEmpty) doc add new Field(field, format(text), highlightFieldType)
      case _ =>
    }
    
    doc
  }

  def getUSPCIndexer(indexDir: File) = new Indexer[UsClass](indexDir, uspcToDoc)
  
}

