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

import scala.language.implicitConversions
import scala.collection.JavaConversions._
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper

/**
 * Constants shared by indexer and searcher.
 */
object Constants {
  val version = Version.LUCENE_48
  
  // the analyzer is used with TextFields (textFieldType), but not with StringFields (keywordFieldType).
  // It uses the following Lucene components:
  // StandardTokenizer, StandardFilter, EnglishPossessiveFilter, LowerCaseFilter, StopFilter, PorterStemFilter.
  val analyzer = new EnglishAnalyzer(version)
  
  // as above, but without stemming
  val unstemmedAnalyzer: Analyzer = new EnglishUnstemmedAnalyzer(version)

  val keywordAnalyzer = new KeywordAnalyzer
  
  private def mkAnalyzer(textFields: List[String], unstemmedFields: List[String]) = {
    val fieldToAnalyzer = (textFields.iterator.map((_, analyzer)) ++ unstemmedFields.iterator.map((_, unstemmedAnalyzer))).toMap
    new PerFieldAnalyzerWrapper(keywordAnalyzer, fieldToAnalyzer)
  }
  
  val cpcAnalyzer = {
    import org.t3as.patClas.common.CPCUtil._
    mkAnalyzer(textFields, unstemmedTextFields)
  }
 
  val ipcAnalyzer = {
    import org.t3as.patClas.common.IPCUtil._
    mkAnalyzer(textFields, unstemmedTextFields)
  }
  
  val uspcAnalyzer = {
    import org.t3as.patClas.common.USPCUtil._
    mkAnalyzer(textFields, unstemmedTextFields)
  }
  
}
