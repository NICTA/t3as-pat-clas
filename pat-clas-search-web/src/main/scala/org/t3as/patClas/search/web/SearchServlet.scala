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

package org.t3as.patClas.search.web

import java.io.OutputStreamWriter

import org.slf4j.LoggerFactory

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.t3as.patClas.api.{CPC, IPC, Search, USPC}
import org.t3as.patClas.api.factory.Factory
import org.t3as.patClas.common.T3asRequestException
import org.t3as.patClas.search.factory.{CPCSearchFactoryImpl, IPCSearchFactoryImpl, USPCSearchFactoryImpl}

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

class CPCSearchServlet extends SearchServlet[CPC.Hit](new CPCSearchFactoryImpl, "CPC")
class IPCSearchServlet extends SearchServlet[IPC.Hit](new IPCSearchFactoryImpl, "IPC")
class USPCSearchServlet extends SearchServlet[USPC.Hit](new USPCSearchFactoryImpl, "USPC")

class SearchServlet[Hit](factory: Factory[Search[Hit]], patClasName: String) extends HttpServlet {
  val log = LoggerFactory.getLogger(getClass)
  val search = factory.create
  val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m
  }

  override def destroy = {
    search.close()
    factory.close()
  }

  override def doGet(in: HttpServletRequest, out: HttpServletResponse) {
    try {
      val q = Option(in.getParameter("q")).getOrElse(throw new T3asRequestException(s"Missing query parameter. Expected /v1.0/${patClasName}?q=your query"))
      val result = search.search(q);

      val callback = Option(in.getParameter("callback"))
      if (log.isDebugEnabled()) log.debug(s"q = $q, callback = $callback")

      out.setContentType("application/json")
      out.setCharacterEncoding("UTF-8")
      val json = mapper.writeValueAsString(search.search(q))
      callback.fold(Iterator(json)) { cb => Iterator(cb, "(", json, ")") } foreach { out.getWriter.print(_) }
    } catch {
      case e: T3asRequestException => {
        log.warn(e.getMessage())
        out.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage())
      }
      case e: Exception => {
        log.error("server error", e)
        out.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }
}
