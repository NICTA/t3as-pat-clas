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

package org.t3as.patClas.api

import scala.language.implicitConversions

object IPC {

  // removed entryType because it is always K
  case class Description(id: Int, symbol: String, level: Int, kind: String, textBody: String)

  case class Hit(score: Float, symbol: String, level: Int, kind: String, textBodyHighlights: String) extends API.HitBase
  
  /** Names of CPC fields in the Lucene index. */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    val Symbol, Level, Kind, TextBody = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val hitFields: Set[String] = Set(Symbol, Level, Kind)
  
  def mkHit(score: Float, f: Map[String, String], h: Map[String, String]) = Hit(score, f(Symbol), f(Level).toInt, f(Kind), h.getOrElse(TextBody, ""))
  
  /** Entity class mapping to a database row representing a IPCEntry
    */
  case class IPCEntry(id: Option[Int], parentId: Int, level: Int, kind: String, symbol: String, endSymbol: Option[String], textBody: String) {

    def toDescription(text: String => String) = Description(id.get, symbol, level, kind, text(textBody))
  }

}
