/*
    Copyright 2014 NICTA
    
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

import java.io.Reader

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.analysis.core.{LowerCaseFilter, StopFilter}
import org.apache.lucene.analysis.standard.{StandardAnalyzer, StandardFilter, StandardTokenizer}
import org.apache.lucene.util.Version

/**
 * Like EnglishAnalyzer, but without the EnglishPossessiveFilter and PorterStemFilter.
 * EnglishAnalyzer uses the following Lucene components:
 * StandardTokenizer, StandardFilter, EnglishPossessiveFilter, LowerCaseFilter, StopFilter, PorterStemFilter.
 */
class EnglishUnstemmedAnalyzer(version: Version) extends Analyzer {

  protected def createComponents(fieldName: String, reader: Reader) = {
    val source: Tokenizer = new StandardTokenizer(version, reader);
    new TokenStreamComponents(
      source,
      new StopFilter(
        version,
        new LowerCaseFilter(version, new StandardFilter(version, source)),
        StandardAnalyzer.STOP_WORDS_SET));
  }
}
