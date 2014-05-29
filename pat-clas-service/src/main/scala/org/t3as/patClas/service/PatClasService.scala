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

package org.t3as.patClas.service

import java.io.File

import javax.ws.rs.{GET, Path, PathParam, Produces, QueryParam}
import javax.ws.rs.core.MediaType

import scala.language.implicitConversions
import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database

import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.LoggerFactory

import org.t3as.patClas.common.Util
import org.t3as.patClas.common.{CPC, IPC, USPC, API}, API.{SearchService, LookupService}
import org.t3as.patClas.common.search.Searcher

object PatClasService {
  import Util.get
  
  val log = LoggerFactory.getLogger(getClass)

  implicit val props = Util.properties("/patClasService.properties")

  val datasource = {
    val ds = new BasicDataSource
    ds.setDriverClassName(get("jdbc.driver"))
    ds.setUsername(get("jdbc.username"))
    ds.setPassword(get("jdbc.password"))
    ds.setMaxTotal(get("jdbc.maxActive").toInt);
    ds.setMaxIdle(get("jdbc.maxIdle").toInt);
    ds.setInitialSize(get("jdbc.initialSize").toInt);
    ds.setValidationQuery(get("jdbc.validationQuery"))
    ds.setPoolPreparedStatements(true) // slick relies on this to reuse prepared statements
    ds.setUrl(get("jdbc.url"))

    // test the data source validity
    ds.getConnection().close()
    ds
  }
  
  val database = Database.forDataSource(datasource)
  
  val slickDriver: JdbcProfile = Util.getObject(get("slick.driver"))
  
  import org.t3as.patClas.common.db.{CPCdb, IPCdb, USPCdb}
  
  val cpcDb = new CPCdb(slickDriver)
  val ipcDb = new IPCdb(slickDriver)
  val uspcDb = new USPCdb(slickDriver)
  
  /** Get a function to transform xml text.
    * If f == Format.XML return the null transform (which preserves the markup), else return toText (which strips out the tags leaving just the text).
   */
  def getToText(f: String) = if ("xml".equalsIgnoreCase(f)) (xml: String) => xml else Util.toText _ 
  
  val cpcSearcher = {
    import CPC._, IndexFieldName._

    // if "field:query" specified leave as is, else search all text fields (accepting a match in any)
    def mkQ(q: String) = if (q.contains(":")) q
      else Seq(ClassTitle, NotesAndWarnings).map(f => s"${f.toString}:(${q})").mkString(" || ")

    new Searcher[Hit](new File(get("cpc.index.path")), ClassTitle, hitFields, mkHit, mkQ)
  }

  val ipcSearcher = {
    import IPC._, IndexFieldName._

    new Searcher[Hit](
      new File(get("ipc.index.path")), TextBody, hitFields, mkHit, (q: String) => q)
  }

  val uspcSearcher = {
    import USPC._, IndexFieldName._

    // if "field:query" specified leave as is, else search all text fields (accepting a match in any)
    def mkQ(q: String) = if (q.contains(":")) q
      else Seq(ClassTitle, SubClassTitle, SubClassDescription, Text).map(f => s"${f.toString}:(${q})").mkString(" || ")
    
    new Searcher[Hit](new File(get("uspc.index.path")), ClassTitle, hitFields, mkHit, mkQ)
  }

  
  def init = {}
  
  def close = {
    log.info("Closing datasource and search indices")
    datasource.close
    cpcSearcher.close
    ipcSearcher.close
    uspcSearcher.close
  }
}
import PatClasService._

@Path("/v1.0/CPC")
class CPCService extends SearchService[CPC.Hit] with LookupService[CPC.Description] {
  
  def searchService: SearchService[CPC.Hit] = this
  def lookupService: LookupService[CPC.Description] = this

  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String) = cpcSearcher.search(q)
  
  @Path("ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def ancestorsAndSelf(@QueryParam("symbol") symbol: String, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      cpcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def children(@QueryParam("parentId") parentId: Int, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      cpcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

@Path("/v1.0/IPC")
class IPCService extends SearchService[IPC.Hit] with LookupService[IPC.Description] {
  
  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String) = ipcSearcher.search(q)

  @Path("ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def ancestorsAndSelf(@QueryParam("symbol") symbol: String, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      ipcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def children(@QueryParam("parentId") parentId: Int, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      ipcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

@Path("/v1.0/USPC")
class USPCService extends SearchService[USPC.Hit] with LookupService[USPC.Description] {
  
  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String) = uspcSearcher.search(q)

  @Path("ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def ancestorsAndSelf(@QueryParam("symbol") symbol: String, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      uspcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("{parentId}/children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def children(@QueryParam("parentId") parentId: Int, @QueryParam("format") format: String) = {
    val fmt = getToText(format)
    database withSession { implicit session =>
      uspcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

