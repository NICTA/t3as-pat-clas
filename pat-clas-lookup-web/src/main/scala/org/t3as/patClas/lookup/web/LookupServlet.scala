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

package org.t3as.patClas.lookup.web

import java.io.OutputStreamWriter

import scala.util.matching.Regex

import org.slf4j.LoggerFactory

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.t3as.patClas.api.{CPC, Format, IPC, Lookup, USPC}
import org.t3as.patClas.api.factory.{Factories, Factory}
import org.t3as.patClas.common.T3asRequestException

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

object Config {
 
  val CPCre = """/([A-Z0-9/]+)""".r // regex for '/' + symbol
  val CPCexample = "B60Y2200/31"    // example of a valid symbol

  val IPCre = """/([A-Z0-9]+)""".r
  val IPCexample = "B61F0015040000"
    
  val USPCre = """/([A-Z0-9/.]+)""".r
  val USPCexample = "105/29.2"
    
  val childRe = """/id/(\d+)/\*""".r // regex for getChilren by parent id
}
import Config._

class CPCLookupServlet extends LookupServlet[CPC.Description](Factories.getCPCLookupFactory(), "CPC", CPCre, CPCexample)
class IPCLookupServlet extends LookupServlet[IPC.Description](Factories.getIPCLookupFactory(), "IPC", IPCre, IPCexample)
class USPCLookupServlet extends LookupServlet[USPC.Description](Factories.getUSPCLookupFactory(), "USPC", USPCre, USPCexample)

class LookupServlet[Description](
    factory: Factory[Lookup[Description]], patClasName: String, symbolRe: Regex, exampleSymbol: String
    ) extends HttpServlet {
  
  val log = LoggerFactory.getLogger(getClass)
  val lookup = factory.create
  val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m
  }

  override def destroy = {
    lookup.close()
    factory.close()
  }

  override def doGet(in: HttpServletRequest, out: HttpServletResponse) {
    try {
      Option(in.getPathInfo) match {
        case Some(symbolRe(symbol)) => write(lookup.getAncestors(symbol, getFormat(in)), Option(in.getParameter("callback")), out)
        case Some(childRe(id)) => write(lookup.getChildren(id.toInt, getFormat(in)), Option(in.getParameter("callback")), out)
        case None => throw new T3asRequestException(s"no pathInfo in request. Expected: /v1.0/${patClasName}/${exampleSymbol}?f=xml")
        case _ => throw new T3asRequestException(s"Invalid request pathInfo: ${in.getPathInfo}. Expected: /v1.0/${patClasName}/${exampleSymbol}?f=xml")
      }
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
  
  private def getFormat(in: HttpServletRequest) =
    if (Option(in.getParameter("f")).getOrElse("") == "xml") Format.XML else Format.PLAIN

  private def write(result: java.util.List[Description], callback: Option[String], out: HttpServletResponse) = {
    out.setContentType("application/json")
    out.setCharacterEncoding("UTF-8")
    val json = mapper.writeValueAsString(result)
    callback.fold(Iterator(json)) { cb => Iterator(cb, "(", json, ")") } foreach { out.getWriter.print(_) }
  }

}
