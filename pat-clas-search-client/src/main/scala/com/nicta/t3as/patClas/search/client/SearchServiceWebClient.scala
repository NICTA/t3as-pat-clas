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

import java.io.Closeable
import java.net.URLEncoder

import scala.collection.JavaConversions.seqAsJavaList

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

import org.t3as.patClas.api.Search

class SearchServiceWebClient[Hit](serverUrl: String, mkHits: String => List[Hit]) extends Search[Hit] with Closeable {
  val log = LoggerFactory.getLogger(getClass)
  val httpClient = HttpClientBuilder.create.build

  override def search(query: String) = {
    val req = new HttpGet(s"${serverUrl}?q=${URLEncoder.encode(query, "UTF-8")}")
    val resp = httpClient.execute(req)
    try {
      val content = EntityUtils.toString(resp.getEntity())
      // if (log.isDebugEnabled) log.debug(s"req = $req, content = $content")
      val list = mkHits(content)
      list
    } finally {
      resp.close
    }
  }

  def close = httpClient.close()
}
