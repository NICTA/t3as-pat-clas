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
import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaMap, mutableSetAsJavaSet, setAsJavaSet }
import scala.collection.mutable.HashSet
import scala.language.postfixOps
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{ DirectoryReader, Term }
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{ IndexSearcher, MultiTermQuery, Query }
import org.apache.lucene.search.postingshighlight.{ DefaultPassageFormatter, PostingsHighlighter }
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.API.HitBase
import scala.util.control.NonFatal
import org.apache.lucene.search.FieldCacheTermsFilter
import org.apache.lucene.util.BytesRef
import org.apache.lucene.search.ConstantScoreQuery
import scala.util.Try
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.queries.ChainedFilter
import org.apache.lucene.search.{ PrefixFilter => lPrefixFilter }
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser

/** Search text associated with a classification code.
  */
class Searcher[Hit <: HitBase](
  defaultFieldsStemmed: List[String],
  defaultFieldsUnstemmed: List[String],
  analyzer: Analyzer,
  fieldsToLoad: Set[String],
  dir: Directory,
  mkHit: (Float, Map[String, String], Map[String, String]) => Hit) extends Closeable {

  val log = LoggerFactory.getLogger(getClass)

  val indexSearcher = new IndexSearcher(DirectoryReader.open(dir))

  // TODO: I wrote my own PrefixFilter which uses DocValues before finding Lucene's one (which is currently used here).
  // Not sure if Lucene's is using DocValues too, but it seems plenty fast enough.
  // If it doesn't need the DocValues field them maybe that should be removed from the index.
  // My version should be deleted if it doesn't get used.
  // def prefixFilter(field: String, prefix: String) = new PrefixFilter(field, prefix.toLowerCase)
  def prefixFilter(field: String, prefix: String) = new lPrefixFilter(new Term(field, prefix.toLowerCase))

  // QueryParser notes on wild cards:
  //
  // Note 1:
  // For a wild-card query (e.g. "Quer*") it doesn't use the analyzer,
  // but (unless you do setLowercaseExpandedTerms(false)) it does lowercase the query.
  // This is handy if the indexing analyzer lowercased it too (e.g. text fields like Title),
  // but causes it to fail on uppercase content (e.g. keyword values like Symbol).
  // The rational for not using the Analyzer is that if you are using a stemming Analyzer,
  // it will make "dogs*" match "doggy". Evidently it gets worse with a German stemmer.
  // So you can:
  // 1) lowercase all your keyword fields, use QueryParser, and have "dogs*" not match "doggy"; or
  // 2) leave keyword fields in the correct case, use AnalyzingQueryParser, and "dogs*" will match "doggy" and German users will be particularly unhappy.
  // The first alternative appears to be generally preferred on forums, so we lower case keywords on indexing and uppercase them on retrieval.
  //
  // Note 2:
  // QueryParser expands a prefix query, like Symbol:A*, to a query term for each matching term in the index.
  // If the number of expanded terms exceeds a configured max, default 1024, an exception is thrown because
  // queries get slow if the number of terms is too large. We handle this exception by using a different QueryParser
  // that creates a filter for the prefix term instead of expanding it, which should be faster.

  def parse(query: String, defaultFields: Array[String]) = {
    log.debug("search: input query = {}", query)

    def setRewrite(qp: QueryParser) = {
      qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
      qp
    }
    
    // Not thread-safe so use a new one each time.
    val qpExpand = setRewrite(new MultiFieldQueryParser(Constants.version, defaultFields, analyzer))

    val q1 = qpExpand.parse(query)
    log.debug("search: parsed query = {}", q1)
    val q2 = Try(q1.rewrite(indexSearcher.getIndexReader())).recover {
      case e: BooleanQuery.TooManyClauses =>
        val qpFilter = setRewrite(new MultiFieldQueryParser(Constants.version, defaultFields, analyzer) {
          override protected def getPrefixQuery(field: String, termStr: String): Query = {
            // use a Filter to implement prefix terms (faster than expansion if there are many matches) 
            new ConstantScoreQuery(prefixFilter(field, termStr))
          }
        })
        val q1 = qpFilter.parse(query)
        log.debug("search: recovering from BooleanQuery.TooManyClauses. parsed query = {}", q1)
        q1.rewrite(indexSearcher.getIndexReader())
    }.get
    log.debug("rewritten query = {}", q2)
    q2
  }

  def search(query: String, stem: Boolean = true, symbolPrefix: Option[String] = None) = {
    val q = parse(query, (if (stem) defaultFieldsStemmed else defaultFieldsUnstemmed).toArray)
    val topDocs = symbolPrefix.map(s => indexSearcher.search(q, new lPrefixFilter(new Term("Symbol", s.toLowerCase)), 50))
      .getOrElse(indexSearcher.search(q, 50))

    log.debug("totalHits = {}", topDocs.totalHits)
    val results = topDocs.scoreDocs.toList
    val docIds = results.map(_.doc)
    val allHlights = highlights(q, docIds).toMap
    // log.debug("allHlights = {}", allHlights.map(e => (e._1, e._2.toList)))
    results.zipWithIndex map {
      case (scoreDoc, idx) =>
        val fields = indexSearcher.doc(scoreDoc.doc, fieldsToLoad).getFields.map(f => f.name -> f.stringValue).toMap
        // log.debug("fields = {}", fields)
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
