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

import java.util.{List => jList}

import scala.collection.JavaConversions.seqAsJavaList

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.API.{LookupService, SearchService}
import org.t3as.patClas.api.javaApi.{Factory => jFactory}

import API.{Factory, LookupService, SearchService}

class TestAPI extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "javaApi adapters" should "convert to java.util.List" in {

    val cpcDescr1 = CPCDescription(1, "symbol1", 1, "classTitle", "notesAndWarnings")    
    val cpcDescr2 = CPCDescription(2, "symbol2", 2, "classTitle", "notesAndWarnings")    
    val cpcHit = CPCHit(0.5f, HitSymbol("A01B1234654321", "A01B12/65"), 3, "classTitle", "notesAndWarnings")
    val cpcSugExact = List("locos", "locomotive", "locomotives")
    val cpcSugFuzzy = List("fuzzy1", "fuzzy2")

    var isClosed = false
    val f = new Factory {
      val cpc = new SearchService[CPCHit] with LookupService[CPCDescription] {
        def ancestorsAndSelf(symbol: String, format: String) = List(cpcDescr1)
        def children(parentId: Int, format: String) = List(cpcDescr2)
        def search(q: String, stem: Boolean, symbol: String) = List(cpcHit) 
        def suggest(prefix: String, num: Int) = Suggestions(cpcSugExact, cpcSugFuzzy)
      }
      
      val ipc = new SearchService[IPCHit] with LookupService[IPCDescription] {
        def ancestorsAndSelf(symbol: String, format: String): List[IPCDescription] = ???
        def children(parentId: Int, format: String): List[IPCDescription] = ???
        def search(q: String, stem: Boolean, symbol: String): List[IPCHit] = ??? 
        def suggest(prefix: String, num: Int): Suggestions = ???
      }
      
      val uspc = new SearchService[USPCHit] with LookupService[USPCDescription] {
        def ancestorsAndSelf(symbol: String, format: String): List[USPCDescription] = ???
        def children(parentId: Int, format: String): List[USPCDescription] = ???
        def search(q: String, stem: Boolean, symbol: String): List[USPCHit] = ??? 
        def suggest(prefix: String, num: Int): Suggestions = ???
      }

      override def close = isClosed = true
    }
    
    val jf = new jFactory(f)
    
    val anc = jf.getCPCLookup.ancestorsAndSelf("sym", "plain")
    anc.size should be(1)
    anc.get(0) should be(cpcDescr1)
    
    val chd = jf.getCPCLookup.children(0, "plain")
    chd.size should be(1)
    chd.get(0) should be(cpcDescr2)

    val src = jf.getCPCSearch.search("loco", false, null)
    src.size should be(1)
    src.get(0) should be(cpcHit)

    val sug = jf.getCPCSearch.suggest("loco", 2)
    val ex: jList[String] = cpcSugExact
    sug.getExact should be(ex)
    val fz: jList[String] = cpcSugFuzzy
    sug.getFuzzy should be(fz)

    isClosed should be(false)
    jf.close
    isClosed should be(true)
  }
}