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

package org.t3as.patClas.client

import org.glassfish.jersey.client.ClientConfig
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.{CPC, IPC, USPC}
import org.t3as.patClas.common.API.{HitBase, LookupService, SearchService}

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType

object PatClasClient {
  val log = LoggerFactory.getLogger(getClass)
  
  val client = {
    val config = (new ClientConfig(classOf[ScalaJacksonJsonProvider])).getConfiguration
    ClientBuilder.newClient(config).target("http://localhost:8080").path("pat-clas-service/rest/v1.0")
  }
  
//  def main(args: Array[String]) {
//    val x = new CPCClient().children(0, "XML")
//    println(s"x = $x")
//  }
}

class PatClasClient[H <: HitBase, D](xpc: String) extends SearchService[H] with LookupService[D] {

  val c = PatClasClient.client.path(xpc)
  
  override def search(q: String) = c.path("search")
    .queryParam("q", q)
    .request(MediaType.APPLICATION_JSON_TYPE)
    .get(classOf[List[H]])
  
  override def ancestorsAndSelf(symbol: String, format: String) = c.path("ancestorsAndSelf")
    .queryParam("symbol", symbol)
    .queryParam("format", format)
    .request(MediaType.APPLICATION_JSON_TYPE)
    .get(classOf[List[D]])

  override def children(parentId: Int, format: String) = c.path("children")
    .queryParam("parentId", parentId.toString)
    .queryParam("format", format)
    .request(MediaType.APPLICATION_JSON_TYPE)
    .get(classOf[List[D]])
  
}

class CPCClient extends PatClasClient[CPC.Hit, CPC.Description]("CPC")
class IPCClient extends PatClasClient[IPC.Hit, IPC.Description]("IPC")
class USPCClient extends PatClasClient[USPC.Hit, USPC.Description]("USPC")
