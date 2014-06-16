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

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.lucene.index.{DocsEnum, TermsEnum}
import org.apache.lucene.search.{DocIdSetIterator, MatchAllDocsQuery}
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.util.Bits
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.IPCHit
import org.t3as.patClas.common.IPCUtil.{hitFields, mkHit, textFields, unstemmedTextFields}
import org.t3as.patClas.common.IPCUtil.IPCEntry
import org.t3as.patClas.common.IPCUtil.IndexFieldName.Symbol
import org.t3as.patClas.common.search.{Constants, Indexer, PrefixFilter, Searcher}

import resource.managed

class TestIndexerFactory extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "IndexerFactory" should "index IPC" in {
    val dir = new RAMDirectory
    for (ipcIndexer <- managed(new Indexer[IPCEntry](Constants.ipcAnalyzer, dir, IndexerFactory.ipcToDoc))) {
      Seq(
        IPCEntry(None, 0, 1, "k", "A01B0012987000", None, "<xml>text body</xml>"),
        IPCEntry(None, 0, 2, "k", "A01B0012986000", None, "<xml>some text</xml>")
      ) foreach ipcIndexer.add
    }

    for (searcher <- managed(new Searcher[IPCHit](textFields, unstemmedTextFields, Constants.cpcAnalyzer, hitFields, dir, mkHit))) {
      {
        val hits = searcher search "text"
        log.debug(s"hits = $hits")
        hits.size should be(2)
      }
      {
        val hits = searcher search """Symbol:A01B12\/9*"""
        log.debug(s"hits = $hits")
        hits.size should be(2)
      }
      {
        
        val c = searcher.indexSearcher.getIndexReader.getContext
        log.debug(s"docBaseInParent = ${c.docBaseInParent},  ordInParent = ${c.ordInParent}, isTopLevel = ${c.isTopLevel}")
        for (cc <- c.leaves) {
          log.debug(s"docBaseInParent = ${cc.docBaseInParent},  ordInParent = ${cc.ordInParent}, isTopLevel = ${cc.isTopLevel}")
          val dv = cc.reader.getSortedDocValues(Symbol)
          val te = dv.termsEnum
          var docsEnum: DocsEnum = null
          val bits = new Bits.MatchAllBits(cc.reader.maxDoc)
          for (term <- termIter(te)) {
            log.debug(s"term = $term")
            
// java.lang.UnsupportedOperationException:
//  at org.apache.lucene.index.SortedDocValuesTermsEnum.docs(SortedDocValuesTermsEnum.java:119)
//  at org.apache.lucene.index.TermsEnum.docs(TermsEnum.java:149)            
//            docsEnum = te.docs(bits, docsEnum)
//            log.debug(s"term = $term, docIds = ${docIter(docsEnum).toList}")
// Also same exception from: te.docFreq
          }
        }
        
        val f = new PrefixFilter(Symbol, "A01B12".toLowerCase)
        val hits = searcher.indexSearcher.search(new MatchAllDocsQuery, f, 100)
        log.debug(s"hits = $hits")
      }
    }
  }

  def termIter(te: TermsEnum) = Iterator.continually(te.next).takeWhile(_ != null).map(b => b.utf8ToString)
  
  def docIter(de: DocsEnum) = Iterator.continually(de.nextDoc).takeWhile(_ !=  DocIdSetIterator.NO_MORE_DOCS)

}

