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

import java.io.Closeable

case class HitSymbol(raw: String, formatted: String)
case class Suggestions(exact: List[String], fuzzy: List[String])

trait HitBase {
  def score: Float
  def symbol: HitSymbol
}

case class CPCDescription(id: Int, symbol: String, level: Int, classTitle: String, notesAndWarnings: String)
case class CPCHit(score: Float, symbol: HitSymbol, level: Int, classTitle: String, notesAndWarnings: String) extends HitBase

// removed entryType because it is always K
case class IPCDescription(id: Int, symbol: String, level: Int, kind: String, textBody: String)
case class IPCHit(score: Float, symbol: HitSymbol, level: Int, kind: String, textBody: String) extends HitBase

case class USPCDescription(id: Int, symbol: String, classTitle: String, subClassTitle: String, subClassDescription: String, text: String)
case class USPCHit(score: Float, symbol: HitSymbol, classTitle: String, subClassTitle: String, subClassDescription: String, text: String) extends HitBase

object API {
  trait SearchService[H <: HitBase] {
    // tried Option[String] for symbol, but Jersey didn't deserialise it in PatClasService
    def search(q: String, stem: Boolean = true, symbol: String = null): List[H]
    def suggest(prefix: String, num: Int): Suggestions
  }

  trait LookupService[D] {
    def ancestorsAndSelf(symbol: String, format: String): List[D]
    def children(parentId: Int, format: String): List[D]
  }

  trait Factory extends Closeable {
    val cpc: SearchService[CPCHit] with LookupService[CPCDescription]
    val ipc: SearchService[IPCHit] with LookupService[IPCDescription]
    val uspc: SearchService[USPCHit] with LookupService[USPCDescription]

    def close = {}
  }

}
