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

package org.t3as.patClas.search

import java.io.{ Closeable, File }
import scala.Array.canBuildFrom
import scala.collection.JavaConversions._
import scala.collection.mutable.{ HashMap, HashSet, MutableList }
import org.apache.lucene.index.{ AtomicReaderContext, DirectoryReader, FieldInfo, StoredFieldVisitor, Term }
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{ Collector, IndexSearcher, Query, Scorer }
import org.apache.lucene.search.postingshighlight.PostingsHighlighter
import org.apache.lucene.store.FSDirectory
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.{ HitBase, Search }
import org.apache.lucene.search.postingshighlight.DefaultPassageFormatter
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter

/** Search text associated with a classification code.
  * @param indexDir path to search index
  * @param defaultSearchField index field to search if none specified in the query (as "{fieldName}:{search term(s)}"
  * @param mkHit how to create a Hit representing a search hit
  * @param mkQuery how to modify the query string if necessary
  */
class SearchService[Hit <: HitBase](
  indexDir: File,
  defaultSearchField: String,
  fieldsToLoad: Set[String],
  mkHit: (Float, Map[String, String], Map[String, String]) => Hit,
  mkQuery: String => String) extends Search[Hit] with Closeable {

  val log = LoggerFactory.getLogger(getClass)
  val searcher = open
  val qparser = new QueryParser(Constants.version, defaultSearchField, Constants.analyzer)

  protected def open = new IndexSearcher(DirectoryReader.open(FSDirectory.open(indexDir)))

  def search(query: String) = {
    val q = {
      log.debug("input query = {}", query)
      val q1 = mkQuery(query)
      log.debug("expanded query = {}", q1)
      val q2 = qparser.parse(q1)
      log.debug("parsed query = {}", q2)
      val q3 = q2.rewrite(searcher.getIndexReader())
      log.debug("rewritten query = {}", q3)
      q3
    }
    val topDocs = searcher.search(q, 50)
    log.debug("totalHits = {}", topDocs.totalHits)
    val results = topDocs.scoreDocs.toList
    val docIds = results.map(_.doc)
    val allHlights = highlights(q, docIds).toMap
    results.zipWithIndex map {
      case (scoreDoc, idx) =>
        val fields = searcher.doc(scoreDoc.doc, fieldsToLoad).getFields.map(f => f.name -> f.stringValue).toMap
        log.debug("fields = {}", fields)
        val hitHlights = for {
          (field, values) <- allHlights
          v <- Option(values(idx))
        } yield field -> v
        mkHit(scoreDoc.score, fields, hitHlights)
    }
  }

  /** @param q query in rewrite form (needed by extractTerms)
    * @return Map field -> array where item i is highlights for docIds[i]
    */
  def highlights(q: Query, docIds: List[Int]) = {

    def getFields(q: Query) = {
      val terms = new HashSet[Term]
      q.extractTerms(terms)
      terms map (_.field()) toArray
    }

    val fieldsIn = getFields(q)
    log.debug("fieldsIn = {}", fieldsIn)
    val maxPassagesIn = fieldsIn map (_ => 3)
    new PostingsHighlighter(1000000) {
      val f = new DefaultPassageFormatter("""<span class="hlight">""", "</span>", "&hellip;<br><br>", false)
      override def getFormatter(field: String) = f
    }.highlightFields(fieldsIn, q, searcher, docIds.toArray, maxPassagesIn).toMap
  }

  def close = searcher.getIndexReader.close
}
