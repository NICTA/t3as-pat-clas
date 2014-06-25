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
import java.util.HashSet

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.JavaConversions.{asScalaBuffer, asScalaSet, mapAsScalaMap, setAsJavaSet}
import scala.language.postfixOps
import scala.util.Try

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.queryparser.classic.{MultiFieldQueryParser, QueryParser}
import org.apache.lucene.search.{BooleanClause, BooleanQuery, ConstantScoreQuery, IndexSearcher, MultiTermQuery, PrefixFilter, Query, TermQuery}
import org.apache.lucene.search.postingshighlight.{DefaultPassageFormatter, PostingsHighlighter}
import org.apache.lucene.store.Directory
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.HitBase
import org.t3as.patClas.common.CPCUtil.IndexFieldName.{HText, HTextUnstemmed, Symbol}


/**
 * Search text associated with a classification code.
 * 
 * TODO: Use of literals Symbol, HText, HTextUnstemmed is a bit hacky and relies on these being the same
 * for all (CPC, IPC and USPC) indices.
 */
class Searcher[Hit <: HitBase](
  defaultFieldsStemmed: List[String],
  defaultFieldsUnstemmed: List[String],
  analyzer: Analyzer,
  fieldsToLoad: List[String],
  dir: Directory,
  mkHit: (Float, Boolean, Map[String, String], Map[String, String]) => Hit) extends Closeable {

  val log = LoggerFactory.getLogger(getClass)

  val indexSearcher = new IndexSearcher(DirectoryReader.open(dir))

  /**
   * @return query with prefix/range etc. queries expanded
   */
  def rewrite(q: Query) = {
    val q2 = q.rewrite(indexSearcher.getIndexReader())
    log.debug("rewrite: {}", q2)
    q2    
  }
  
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
    Try(rewrite(q1)).recover {
      case e: BooleanQuery.TooManyClauses =>
        val qpFilter = setRewrite(new MultiFieldQueryParser(Constants.version, defaultFields, analyzer) {
          override protected def getPrefixQuery(field: String, termStr: String): Query = {
            // use a Filter to implement prefix terms (faster than expansion if there are many matches) 
            new ConstantScoreQuery(new PrefixFilter(new Term(field, termStr.toLowerCase)))
          }
        })
        val q1 = qpFilter.parse(query)
        log.debug("search: recovering from BooleanQuery.TooManyClauses. parsed query = {}", q1)
        rewrite(q1)
    }.get // Exception if recovery failed
  }
  
  /**
   * @return set of Terms in q
   */
  def getTerms(q: Query) = {
    val terms = new HashSet[Term]
    q.extractTerms(terms)
    terms.toSet
  }
  
  /**
   * @return a new Query with field names mapped (1 to many) according to mapField
   */
  def mapQuery(q: Query)(mapField: String => List[String]) = {
    val q2 = getTerms(q).foldLeft(new BooleanQuery) { (q2, t) => {
      mapField(t.field).foreach {
        f => q2.add(new TermQuery(new Term(f, t.bytes)), BooleanClause.Occur.SHOULD)
      }
      q2
    }}
    log.debug("mapQuery: {}", q2)
    q2
  }
  
  /**
   * @return transformed query for highlighting, mapping non-highlightable fields to highlightable ones
   */
  def highlightQuery(q: Query) = {     
    val textFields = (defaultFieldsStemmed ++ defaultFieldsUnstemmed).toSet
    val q2 = rewrite(mapQuery(q) { f: String => 
      if (textFields.contains(f)) List(f) // unchanged
      else if (HText.toString == f) defaultFieldsStemmed // map to all stemmed fields
      else if (HTextUnstemmed.toString == f) defaultFieldsUnstemmed // map to all unstemmed fields
      else List.empty // don't highlight non-text fields like Symbol (not useful and they are not indexed with offsets for highlighting)
    })
    log.debug("highlightQuery: {}", q2)
    q2
  }
  
  /**
   * If the query contains HText, HTextUnstemmed terms then add extra terms to ensure a hit must include
   * at least one of these terms in it's direct text (not only in the ancestor text),
   * otherwise return the query unchanged. 
   */
  def hierarchyQuery(q: Query) = {
    val q2 = mapQuery(q) { f: String => 
      if (HText.toString == f) defaultFieldsStemmed // map to all stemmed fields
      else if (HTextUnstemmed.toString == f) defaultFieldsUnstemmed // map to all unstemmed fields
      else List.empty // ignore
    }
    val q3 = if (q2.toString().isEmpty) {
      q
    } else {
      val q3 = new BooleanQuery
      q3.add(q, BooleanClause.Occur.MUST)
      q3.add(q2, BooleanClause.Occur.MUST)
      q3
    }
    log.debug("hierarchyQuery: {}", q3)
    q3
  }

  def search(query: String, stem: Boolean = true, symbolPrefix: Option[String] = None) = {
    val defaultFields = if (stem) defaultFieldsStemmed else defaultFieldsUnstemmed
    val q = hierarchyQuery(parse(query, defaultFields.toArray))
    val topDocs = symbolPrefix.map(s => indexSearcher.search(q, new PrefixFilter(new Term(Symbol, s.toLowerCase)), 50))
      .getOrElse(indexSearcher.search(q, 50))

    log.debug("totalHits = {}", topDocs.totalHits)
    val results = topDocs.scoreDocs.toList
    val docIds = results.map(_.doc)
    
    val allHlights = highlights(highlightQuery(q), docIds).toMap
    // log.debug("allHlights = {}", allHlights.map(e => (e._1, e._2.toList)))
    // A query could have terms for both stemmed and unstemmed versions of the same field, but we can only return a
    // single highlighted field. In such a case if stemming is selected mkHit will prefer the stemmed highlights and vice-versa.  
    val f2load = (fieldsToLoad ++ defaultFields).toSet
    results.zipWithIndex map {
      case (scoreDoc, idx) =>
        val fields = indexSearcher.doc(scoreDoc.doc, f2load).getFields.map(f => f.name -> f.stringValue).toMap
        // log.debug("fields = {}", fields)
        val hitHlights = for {
          (field, values) <- allHlights
          v <- Option(values(idx))
        } yield field -> v
        mkHit(scoreDoc.score, stem, fields, hitHlights)
    }
  }

  /** @param q query (must be in rewrite form for extractTerms)
    * @return Map field -> array where item i is highlights for docIds[i]
    */
  def highlights(q: Query, docIds: List[Int]): Map[String, Array[String]] = {
    val re = """<\/?span[^>]*>|&hellip;<br><br>"""r

    val fieldsIn = getTerms(q).map(_.field).toArray
    log.debug("fieldsIn = {}", fieldsIn)
    if (docIds.isEmpty || fieldsIn.isEmpty) Map.empty
    else {
      val ph = new PostingsHighlighter(1000000) {
        val f = new DefaultPassageFormatter("""<span class="hlight">""", "</span>", "&hellip;<br><br>", false)
        override def getFormatter(field: String) = f
      }
      val maxPassagesIn = fieldsIn map (_ => 3)
      val re = """<\/?span[^>]*>"""r
      val x = ph.highlightFields(fieldsIn, q, indexSearcher, docIds.toArray, maxPassagesIn).toMap
      x
    }
  }

  def close = indexSearcher.getIndexReader.close
}
