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

import java.io.{ BufferedInputStream, BufferedOutputStream, File, FileInputStream, FileOutputStream }

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.lucene.search.suggest.Lookup
import org.apache.lucene.search.suggest.analyzing.{ AnalyzingSuggester, FuzzySuggester }

import Constants.version
import resource.managed

object Suggest {
  //    def toSuggestionHighlight(n: Int): toSuggestion = r => {
  //      val s = r.key.toString
  //      val m = min(n, s.length)
  //      Suggestion(0L, "<b>" + s.substring(0, m) + "</b>" + s.substring(m))
  //    }

  val unStemmedAnalyzer = new EnglishUnstemmedAnalyzer(version)


  private def append(f: File, suffix: String) = new File(f.getParentFile, f.getName() + suffix)
  def fuzzySugFile(indexDir: File) = append(indexDir, "FuzzySug")
  def exactSugFile(indexDir: File) = append(indexDir, "ExactSug")
}
import Suggest.unStemmedAnalyzer

trait Suggest {
  val s: Lookup
  def lookup(key: String, num: Int) = s.lookup(key, false, num).toList.map { r =>
    val s = r.key.toString
    val m = Math.min(key.length, s.length)
    "<b>" + s.substring(0, m) + "</b>" + s.substring(m)
  }
}

trait LoadableSuggest extends Suggest {
  val s: AnalyzingSuggester

  val build = s.build _

  /** Once built you can store the data structure to avoid having to build it again in another process. */
  def store(f: File) = for (o <- managed(new BufferedOutputStream(new FileOutputStream(f)))) s.store(o)

  /** Load the previously stored data structure. */
  def load(f: File) = for (i <- managed(new BufferedInputStream(new FileInputStream(f)))) s.load(i)
}

//  class InfixSuggest(sugDir: File) extends Suggest with Closeable {
//    val s = new AnalyzingInfixSuggester(version, sugDir, unStemmedAnalyzer, unStemmedAnalyzer, 2)
//    
//    val build = s.build _
//    val close = s.close
//  }

class ExactSuggest extends LoadableSuggest {
  val s = new AnalyzingSuggester(unStemmedAnalyzer)
}

class FuzzySuggest extends LoadableSuggest {
  val s = new FuzzySuggester(unStemmedAnalyzer)
}