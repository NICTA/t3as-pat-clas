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

import java.io.Closeable
import java.net.URLEncoder

import scala.collection.JavaConversions.seqAsJavaList

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

import org.t3as.patClas.api.{Format, Lookup}

class LookupWebClient[Desc](serverUrl: String, mkDesc: String => List[Desc]) extends Lookup[Desc] with Closeable {
  val log = LoggerFactory.getLogger(getClass)
  val httpClient = HttpClientBuilder.create.build

  override def getAncestors(symbol: String, f: Format) = doit(symbol, f)
  override def getChildren(parentId: Int, f: Format) = doit(s"id/$parentId/*", f)
  
  private def doit(symbol: String, f: Format) = {
    def en(s: String) = URLEncoder.encode(s, "UTF-8")
    val req = new HttpGet(serverUrl.format(en(symbol), if (f == Format.XML) "xml" else "plain"))
    val resp = httpClient.execute(req)
    try {
      val content = EntityUtils.toString(resp.getEntity())
      if (log.isDebugEnabled) log.debug(s"content = $content")
      mkDesc(content)
    } finally {
      resp.close
    }
  }

  def close = httpClient.close()
}
