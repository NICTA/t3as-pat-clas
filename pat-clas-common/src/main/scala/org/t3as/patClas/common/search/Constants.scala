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
  private val analyzer: Analyzer = new EnglishAnalyzer(version)
  private val keywordAnalyzer = new KeywordAnalyzer
  
  private def mkAnalyzer(textFields: String*) = {
    val x = textFields.map((_, analyzer)).toMap
    new PerFieldAnalyzerWrapper(keywordAnalyzer, x)
  }
  
  val cpcAnalyzer = {
    import org.t3as.patClas.api.CPC.IndexFieldName._
    mkAnalyzer(ClassTitle, NotesAndWarnings)
  }
 
  val ipcAnalyzer = {
    import org.t3as.patClas.api.IPC.IndexFieldName._
    mkAnalyzer(TextBody)
  }
  
  val uspcAnalyzer = {
    import org.t3as.patClas.api.USPC.IndexFieldName._
    mkAnalyzer(ClassTitle, SubClassTitle, SubClassDescription, Text)
  }
  
}
