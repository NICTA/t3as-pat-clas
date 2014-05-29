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

package org.t3as.patClas.common.javaApi

import java.util.List
import scala.collection.JavaConversions.seqAsJavaList
import org.t3as.patClas.common.API.{HitBase, LookupService => LS, SearchService => SS}
import org.t3as.patClas.common.{CPC, IPC, USPC}
import java.io.Closeable

class LookupService[D](l: LS[D]) {
  def ancestorsAndSelf(symbol: String, format: String): List[D] = l.ancestorsAndSelf(symbol, format)
  def children(parentId: Int, format: String): List[D] = l.children(parentId, format)
}

class SearchService[H <: HitBase](s: SS[H]) {
  def search(q: String): List[H] = s.search(q)
}

class Factory(
    cpc: SS[CPC.Hit] with LS[CPC.Description], 
    ipc: SS[IPC.Hit] with LS[IPC.Description], 
    uspc: SS[USPC.Hit] with LS[USPC.Description]) extends Closeable {
  
  def getCPCLookup = new LookupService(cpc)
  def getIPCLookup = new LookupService(ipc)
  def getUSPCLookup = new LookupService(uspc)
  
  def getCPCSearch = new SearchService(cpc)
  def getIPCSearch = new SearchService(ipc)
  def getUSPCSearch = new SearchService(uspc)
  
  def close = {}
}