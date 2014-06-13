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

  case class Hit(score: Float, symbol: API.Symbol, level: Int, kind: String, textBodyHighlights: String) extends API.HitBase
  
  // TODO: move IndexFieldName, textFields, hitFields, toCpcFormat, mkHit to common.IPCUtil
  /**
   * Names of IPC fields in the Lucene index.
   * 
   * IPC symbols in the source data have format:
   *   A99AZMMMGGGGGZ (Z = zero padded, ZMMM = 4 digit left padded main group, GGGGGZ = 6 digit right padded sub group)
   * which is harder to read and inconsistent with the format of ref's to IPCs in the CPC:
   *   A99AMM/GG.
   * We display and allow searching in this second format stored in the Symbol field; but also store the original format in SymbolRaw
   * because that format is used in database lookups.
   */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    val Symbol, SymbolRaw, Level, Kind, TextBody, TextBodyUnstemmed = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val textFields: List[String] = List(TextBody)
  val unstemmedTextFields: List[String] = List(TextBodyUnstemmed) // in pref order for suggester
  val hitFields: Set[String] = Set(Symbol, SymbolRaw, Level, Kind, TextBody)
  
  private val re = """(\p{Upper}\p{Digit}{2}\p{Upper})(\p{Digit}{4})(\p{Digit}{6})""".r
  
  def toCpcFormat(s: String) = {
    import API.{ltrim, rtrim}
    
    if (s.length != 14) s
    else {
      s match {
        case re(sectionClassSubclass, mainGroup, subGroup) => {
          val sg = rtrim(subGroup, '0')
          if (sg.isEmpty) sectionClassSubclass + ltrim(mainGroup, '0')
          else sectionClassSubclass + ltrim(mainGroup, '0') + '/' + sg
        }
        case _ => s
      }
    }
  }
  
  def mkHit(score: Float, f: Map[String, String], h: Map[String, String]) = {
    def getH(s: String) = h.getOrElse(s, f.getOrElse(s, ""))
    def getHU(s: String) = h.getOrElse(s, f.getOrElse(s, "").toUpperCase)
    Hit(score, API.Symbol(f(SymbolRaw), getHU(Symbol)), f(Level).toInt, f(Kind), getH(TextBody))
  }
  
  /** Entity class mapping to a database row representing a IPCEntry
    */
  case class IPCEntry(id: Option[Int], parentId: Int, level: Int, kind: String, symbol: String, endSymbol: Option[String], textBody: String) {

    def toDescription(text: String => String) = Description(id.get, symbol, level, kind, text(textBody))
  }

}
