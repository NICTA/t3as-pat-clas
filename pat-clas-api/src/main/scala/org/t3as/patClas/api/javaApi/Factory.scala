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

package org.t3as.patClas.api.javaApi

import java.io.Closeable
import java.util.List

import scala.collection.JavaConversions.seqAsJavaList

import org.t3as.patClas.api.{CPC, IPC, USPC}
import org.t3as.patClas.api.API.{Factory => SF, HitBase, LookupService => LS, SearchService => SS}


class LookupAdapter[D](l: LS[D]) {
  def ancestorsAndSelf(symbol: String, format: String): List[D] = l.ancestorsAndSelf(symbol, format)
  def children(parentId: Int, format: String): List[D] = l.children(parentId, format)
}

trait Suggestions {
  def getExact: List[String]
  def getFuzzy: List[String]
}

class SearchAdapter[H <: HitBase](s: SS[H]) {
  def search(q: String, stem: Boolean = true, symbol: String = null): List[H] = s.search(q, stem, symbol)
  def suggest(prefix: String, num: Int): Suggestions = new Suggestions {
    val x = s.suggest(prefix, num)
    override def getExact = x.exact
    override def getFuzzy = x.fuzzy 
  }
}

class Factory(f: SF) extends Closeable {
  
  def getCPCLookup = new LookupAdapter(f.cpc)
  def getIPCLookup = new LookupAdapter(f.ipc)
  def getUSPCLookup = new LookupAdapter(f.uspc)
  
  def getCPCSearch = new SearchAdapter[CPC.Hit](f.cpc)
  def getIPCSearch = new SearchAdapter[IPC.Hit](f.ipc)
  def getUSPCSearch = new SearchAdapter[USPC.Hit](f.uspc)
  
  def close = f.close
}