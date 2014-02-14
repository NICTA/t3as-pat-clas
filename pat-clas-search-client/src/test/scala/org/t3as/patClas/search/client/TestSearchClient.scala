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

package org.t3as.patClas.search.client

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import org.t3as.patClas.search.factory.CPCSearchFactoryImpl
import org.t3as.patClas.search.factory.IPCSearchFactoryImpl
import org.t3as.patClas.api.factory.Factories

class TestSearchClient extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)
  
  // Tests require a server to be running at http://127.0.0.1:8080/pat-clas-lookup-web/
  // so they are disabled with 'ignore'. Change comment on lines below to run. 
  
  ignore should "fetch and deserialize CPC" in {
  // "CPCSearchClient" should "fetch and deserialize CPC" in {
    val f = Factories.getCPCSearchFactory
    val c = f.create
    val list = c.search("anti-biotic resistance")
    log.debug("CPC list = {}", list)
    c.close
    f.close
  }

  ignore should "fetch and deserialize IPC" in {
  // "IPCSearchClient" should "fetch and deserialize IPC" in {
    val f = Factories.getIPCSearchFactory
    val c = f.create
    val list = c.search("anti-biotic resistance")
    log.debug("IPC list = {}", list)
    c.close
    f.close
  }

}
