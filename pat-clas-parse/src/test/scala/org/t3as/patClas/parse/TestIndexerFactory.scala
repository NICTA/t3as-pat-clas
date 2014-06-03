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

import org.apache.lucene.store.RAMDirectory
import org.scalatest.{ FlatSpec, Matchers }
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.IPC.{ Hit, IPCEntry, hitFields, mkHit, textFields }
import org.t3as.patClas.common.search.{ Constants, Indexer, Searcher }

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

    for (searcher <- managed(new Searcher[Hit](textFields, Constants.cpcAnalyzer, hitFields, dir, mkHit))) {
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
    }
  }

}

