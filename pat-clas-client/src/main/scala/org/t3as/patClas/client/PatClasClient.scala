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
import org.t3as.patClas.api.{CPCDescription, CPCHit, HitBase, IPCDescription, IPCHit, Suggestions, USPCDescription, USPCHit}
import org.t3as.patClas.api.API.{Factory, LookupService, SearchService}
import org.t3as.patClas.api.javaApi.{Factory => JF}

import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType

// path = "http://localhost:8080/pat-clas-service/rest/v1.0"
class PatClasClient(path: String) extends Factory {
  val log = LoggerFactory.getLogger(getClass)
  val config = (new ClientConfig(classOf[ScalaJacksonJsonProvider])).getConfiguration

  // path = "http://localhost:8080/pat-clas-service/rest/v1.0/CPC"
  class Client[H <: HitBase, D](path: String) extends SearchService[H] with LookupService[D] {
    val c = ClientBuilder.newClient(config).target(path)

    override def search(q: String, stem: Boolean = true, symbol: String = null) = { 
      val x = c.path("search").queryParam("q", q).queryParam("stem", stem.toString)
      Option(symbol).map(s => x.queryParam("symbol", s)).getOrElse(x)
        .request(MediaType.APPLICATION_JSON_TYPE).get(classOf[Array[H]]).toList
    }
    
    override def suggest(prefix: String, num: Int) = c.path("search")
      .queryParam("prefix", prefix)
      .queryParam("num", num.toString)
      .request(MediaType.APPLICATION_JSON_TYPE)
      .get(classOf[Suggestions])

    override def ancestorsAndSelf(symbol: String, format: String) = c.path("ancestorsAndSelf")
      .queryParam("symbol", symbol)
      .queryParam("format", format)
      .request(MediaType.APPLICATION_JSON_TYPE)
      .get(classOf[Array[D]]).toList

    override def children(parentId: Int, format: String) = c.path("children")
      .queryParam("parentId", parentId.toString)
      .queryParam("format", format)
      .request(MediaType.APPLICATION_JSON_TYPE)
      .get(classOf[Array[D]]).toList
  }

  val cpc = new Client[CPCHit, CPCDescription](path + "/CPC")

  val ipc = new Client[IPCHit, IPCDescription](path + "/IPC")

  val uspc = new Client[USPCHit, USPCDescription](path + "/USPC")
}

object PatClasClient {
  import org.t3as.patClas.api.javaApi.{Factory => JF}
  def toJavaApi(path: String) = new JF(new PatClasClient(path))
}
