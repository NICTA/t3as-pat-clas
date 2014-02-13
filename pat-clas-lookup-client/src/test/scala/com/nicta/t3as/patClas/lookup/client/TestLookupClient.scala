/*
    Copyright 2013 NICTA
    
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

package org.t3as.patClas.lookup.client

import scala.collection.JavaConversions._

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import org.t3as.patClas.api.Format
import org.t3as.patClas.api.factory.Factories

class TestLookupClient extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)
  
  // Test requires a server to be running at http://127.0.0.1:8080/pat-clas-lookup-web/
  // so it is disable with 'ignore', change comment on lines below to run it. 
  ignore should "fetch and deserialize CPC" in {
  // "CPC LookupClient" should "fetch and deserialize CPC" in {
    val factory = Factories.getCPCLookupFactory()
    val client = factory.create
    val list = client.getAncestors("A01B", Format.XML) 
    log.debug("CPC list = {}", list)
    list.size should be (4)
    list zip Seq(("A", 2), ("A01", 3), ("A01", 4), ("A01B", 5)) foreach { case(d, (symbol, level)) =>
      d.symbol should be (symbol)
      d.level should be (level)
    }
    client.close()
    factory.close()
  }

  ignore should "fetch and deserialize IPC" in {
  // "IPC LookupClient" should "fetch and deserialize IPC" in {
    val factory = Factories.getIPCLookupFactory()
    val client = factory.create
    val list = client.getAncestors("A01B", Format.XML) 
    log.debug("IPC list = {}", list)
//    list.size should be (4)
//    list zip Seq(("A", 2), ("A01", 3), ("A01", 4), ("A01B", 5)) foreach { case(d, (symbol, level)) =>
//      d.symbol should be (symbol)
//      d.level should be (level)
//    }
    client.close()
    factory.close()
  }

}
