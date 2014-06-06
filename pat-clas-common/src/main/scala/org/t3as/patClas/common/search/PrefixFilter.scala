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

import org.apache.lucene.util.BytesRef
import org.apache.lucene.search.Filter
import org.apache.lucene.search.FieldCache
import org.apache.lucene.index.AtomicReaderContext
import org.apache.lucene.search.DocIdSet
import org.apache.lucene.util.Bits
import org.apache.lucene.util.FixedBitSet
import org.apache.lucene.index.SortedDocValues
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.index.TermsEnum
import org.apache.lucene.search.FieldCacheDocIdSet
import org.slf4j.LoggerFactory

/** A Filter replacement for a prefix query which throws:
  * org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024
  * because the wildcard matches too many terms.
  *
  * Based on org.apache.lucene.search.FieldCacheTermsFilter.
  * 
  * TODO: Check that org.apache.lucene.search.PrefixFilter doesn't do this job.
  * I think it does not use SortedDocValues and cannot handle more than maxClauseCount matches.
  */
class PrefixFilter(field: String, prefix: String) extends Filter {
  val log = LoggerFactory.getLogger(getClass)
  
  val EmptyDocIdSet = new DocIdSet {
    override def iterator: DocIdSetIterator = null
  }
  
  // if a SortedDocValuesField was created at indexing time that is used,
  // otherwise a SortedDocValues is dynamically created on first use and cached
  def getFieldCache = FieldCache.DEFAULT
  
  def termIter(te: TermsEnum) = Iterator.continually(te.next).takeWhile(_ != null).map(b => b.utf8ToString)

  override def getDocIdSet(context: AtomicReaderContext, acceptDocs: Bits): DocIdSet = {
    import TermsEnum.SeekStatus.END
    val fcsi = getFieldCache.getTermsIndex(context.reader, field)
    val te = fcsi.termsEnum
    val b = new BytesRef(prefix)
    val range: Option[(Int, Int)] = te.seekCeil(b) match {
      case END => None
      case _ => {
        val t = te.term.utf8ToString
        // log.debug(s"getDocIdSet: t = $t, ord = ${te.ord}")
        if (t.startsWith(prefix)) {
          val str = te.ord.toInt
          for (t <- termIter(te).takeWhile(_.startsWith(prefix))) {
            // log.debug(s"getDocIdSet: t = $t, ord = ${te.ord}");
          }
          Some(str, te.ord.toInt)
        } else None
      }
    }
    range.map {
      case (str, end) => { 
        // log.debug(s"getDocIdSet: str = $str, end = $end")
        new FieldCacheDocIdSet(context.reader().maxDoc(), acceptDocs) {
          override protected def matchDoc(doc: Int) = {
            val ord = fcsi.getOrd(doc)
            // log.debug(s"matchDoc($doc): ord = $ord");
            str <= ord && ord < end 
          }
        }
      }
    }.getOrElse {
      // log.debug(s"getDocIdSet: no matching range");
      EmptyDocIdSet
    }
  }
}
