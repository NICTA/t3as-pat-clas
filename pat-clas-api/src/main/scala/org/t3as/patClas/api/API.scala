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

object API {

  // TODO: move API.{l,r}trim to common.Util (after moving IPC.toCpcFormat to common)
  /** trim leading c's from s */
  def ltrim(s: String, c: Char) = {
    val i = s.indexWhere(_ != c)
    if (i == -1) "" else s.substring(i)
  }
  
  /** trim trailing c's from s */
  def rtrim(s: String, c: Char) = {
    val i = s.lastIndexWhere(_ != c)
    s.substring(0, i + 1)
  }

  case class Symbol(raw: String, formatted: String)
  case class Suggestions(exact: List[String], fuzzy: List[String])

  trait HitBase {
    def score: Float
    def symbol: Symbol
  }

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
    val cpc: SearchService[CPC.Hit] with LookupService[CPC.Description]
    val ipc: SearchService[IPC.Hit] with LookupService[IPC.Description]
    val uspc: SearchService[USPC.Hit] with LookupService[USPC.Description]

    def close = {}
  }
}
