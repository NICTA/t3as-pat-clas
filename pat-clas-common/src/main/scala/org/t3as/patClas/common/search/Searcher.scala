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
import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.JavaConversions.{asScalaBuffer, mapAsScalaMap, mutableSetAsJavaSet, setAsJavaSet}
import scala.collection.mutable.HashSet
import scala.language.postfixOps
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{IndexSearcher, MultiTermQuery, Query}
import org.apache.lucene.search.postingshighlight.{DefaultPassageFormatter, PostingsHighlighter}
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.API.HitBase
import scala.util.control.NonFatal
import org.apache.lucene.search.FieldCacheTermsFilter
import org.apache.lucene.util.BytesRef

/** Search text associated with a classification code.
  * @param indexDir path to search index
  * @param defaultSearchField index field to search if none specified in the query (as "{fieldName}:{search term(s)}"
  * @param mkHit how to create a Hit representing a search hit
  * @param mkQuery how to modify the query string if necessary
  */
class Searcher[Hit <: HitBase](
  defaultFields: Array[String],
  analyzer: Analyzer,
  fieldsToLoad: Set[String],
  dir: Directory,
  mkHit: (Float, Map[String, String], Map[String, String]) => Hit) extends Closeable {

  val log = LoggerFactory.getLogger(getClass)

  val indexSearcher = new IndexSearcher(DirectoryReader.open(dir))

  // QueryParser note on wild cards:
  // For a wild-card query (e.g. "Quer*") it doesn't use the analyzer,
  // but (unless you do setLowercaseExpandedTerms(true)) it does lowercase the query.
  // This is handy if the indexing analyzer lowercased it too (e.g. text fields like Title),
  // but causes it to fail on uppercase content (e.g. keyword values like Symbol).
  // The rational for not using the Analyzer is that if you are using a stemming Analyzer,
  // it will make "dogs*" match "doggy". Evidently it gets worse with a German stemmer.
  // So you can:
  // 1) lowercase all your keyword fields, use QueryParser, and have "dogs*" not match "doggy"; or
  // 2) leave keyword fields in the correct case, use AnalyzingQueryParser, and "dogs*" will match "doggy" and German users will be particularly unhappy.
  // The first alternative appears to be generally preferred on forums, so we lower case keywords on indexing and uppercase them on search.

  // Not thread-safe so use a new one each time.
  def qparser = {
    val qp = new MultiFieldQueryParser(Constants.version, defaultFields, analyzer) {
      override protected def getPrefixQuery(field: String, termStr: String): Query = {
        val q = super.getPrefixQuery(field, termStr)
        log.debug(s"MultiFieldQueryParser: field = $field, termStr = $termStr, q = $q")
        q
      } 
    }
    qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
    qp
  }

  def search(query: String, symbolPrefix: Option[String] = None) = {
    val q = {
      log.debug("input query = {}", query)
      val q1 = qparser.parse(query)
      log.debug("parsed query = {}", q1)
      val q2 = q1.rewrite(indexSearcher.getIndexReader())
      log.debug("rewritten query = {}", q2)
      q2
    }
    
    val topDocs = symbolPrefix.map(s => indexSearcher.search(q, new PrefixFilter("Symbol", s.toLowerCase), 50))
      .getOrElse(indexSearcher.search(q, 50))
      
    log.debug("totalHits = {}", topDocs.totalHits)
    val results = topDocs.scoreDocs.toList
    val docIds = results.map(_.doc)
    val allHlights = highlights(q, docIds).toMap
    log.debug("allHlights = {}", allHlights.map(e => (e._1, e._2.toList)))
    results.zipWithIndex map {
      case (scoreDoc, idx) =>
        val fields = indexSearcher.doc(scoreDoc.doc, fieldsToLoad).getFields.map(f => f.name -> f.stringValue).toMap
        log.debug("fields = {}", fields)
        val hitHlights = for {
          (field, values) <- allHlights
          v <- Option(values(idx))
        } yield field -> v
        mkHit(scoreDoc.score, fields, hitHlights)
    }
  }

  /** @param q query (must be in rewrite form for extractTerms)
    * @return Map field -> array where item i is highlights for docIds[i]
    */
  def highlights(q: Query, docIds: List[Int]): Map[String, Array[String]] = {

    def getFields(q: Query) = {
      val terms = new HashSet[Term]
      q.extractTerms(terms)
      terms.map(_.field()).toArray
    }

    val re = """<\/?span[^>]*>|&hellip;<br><br>"""r

    def symbolToUpper(t: String) = {
      val buf = new StringBuilder
      val end = re.findAllMatchIn(t).foldLeft(0) {
        case (pos, m) =>
          buf append t.substring(pos, m.start).toUpperCase append m.matched
          m.end
      }
      buf append t.substring(end).toUpperCase
      buf.toString
    }
    
    def highlighSymbolToUpper(e: (String, Array[String])) =
      if ("Symbol" == e._1) (e._1, e._2.map(symbolToUpper))
      else e
    
    val fieldsIn = getFields(q)
    log.debug("fieldsIn = {}", fieldsIn)
    if (docIds.isEmpty || fieldsIn.isEmpty) Map.empty
    else {
      val ph = new PostingsHighlighter(1000000) {
        val f = new DefaultPassageFormatter("""<span class="hlight">""", "</span>", "&hellip;<br><br>", false)
        override def getFormatter(field: String) = f
      }
      val maxPassagesIn = fieldsIn map (_ => 3)
      val re = """<\/?span[^>]*>"""r
      val x = ph.highlightFields(fieldsIn, q, indexSearcher, docIds.toArray, maxPassagesIn).toMap.map(highlighSymbolToUpper)
      x
    }
  }

  def close = indexSearcher.getIndexReader.close
}
