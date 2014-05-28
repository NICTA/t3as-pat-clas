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

import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.CPC.IndexFieldName.{ClassTitle, NotesAndWarnings, Symbol, convert}

import resource.managed

class TestIndexer extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "Indexer" should "make searchable index" in {
    val dir = RAMIndex.makeTestIndex
    
    for (reader <- managed(DirectoryReader.open(dir))) {
      reader.numDocs should be (4)
      val searcher: IndexSearcher = new IndexSearcher(reader)
      
      {
      val td = searcher.search(new TermQuery(new Term(Symbol, "B29C31/00")), 10)
      td.totalHits should be (2)
      }
      
      {
      val qp = new QueryParser(Constants.version, ClassTitle, Constants.analyzer)
      val q = qp.parse("FAKED")
      log.debug("q = {}", q)
      val td = searcher.search(q, 10) // analyzer should make this match "faking"
      td.totalHits should be (3)
      }
      
      {
      val qp = new QueryParser(Constants.version, NotesAndWarnings, Constants.analyzer)
      val q = qp.parse("Attention")
      log.debug("q = {}", q)
      val td = searcher.search(q, 10)
      td.totalHits should be (1)
      }
    }
  }

}

