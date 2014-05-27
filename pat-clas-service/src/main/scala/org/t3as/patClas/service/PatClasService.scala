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
import scala.language.implicitConversions
import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.JdbcBackend.Database
import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.Util
import javax.ws.rs.{GET, Path, PathParam, Produces, QueryParam}
import javax.ws.rs.core.MediaType
import PatClasService._
import org.t3as.patClas.common.search.Searcher

// TODO: how do we set a shutdown hook to close the database DataSource and Lucene Searcher?
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
    import org.t3as.patClas.common.CPCTypes, CPCTypes.IndexFieldName.{ClassTitle, NotesAndWarnings, convert}

    // if "field:query" specified leave as is, else search all text fields (accepting a match in any)
    def mkQ(q: String) = if (q.contains(":")) q
      else Seq(ClassTitle, NotesAndWarnings).map(f => s"${f.toString}:(${q})").mkString(" || ")

    new Searcher[CPCTypes.Hit](
      new File(get("cpc.index.path")),
      ClassTitle,
      CPCTypes.hitFields,
      CPCTypes.mkHit _,
      mkQ
      )
  }

  val ipcSearcher = {
    import org.t3as.patClas.common.IPCTypes, IPCTypes.IndexFieldName.{TextBody, convert}

    new Searcher[IPCTypes.Hit](
      new File(get("ipc.index.path")),
      TextBody,
      IPCTypes.hitFields,
      IPCTypes.mkHit _,
      (q: String) => q
      )
  }

  val uspcSearcher = {
    import org.t3as.patClas.common.USPCTypes, USPCTypes.IndexFieldName.{ClassTitle, SubClassTitle, SubClassDescription, Text, convert}

    // if "field:query" specified leave as is, else search all text fields (accepting a match in any)
    def mkQ(q: String) = if (q.contains(":")) q
      else Seq(ClassTitle, SubClassTitle, SubClassDescription, Text).map(f => s"${f.toString}:(${q})").mkString(" || ")
    
    new Searcher[USPCTypes.Hit](
      new File(get("uspc.index.path")),
      ClassTitle,
      USPCTypes.hitFields,
      USPCTypes.mkHit _,
      mkQ
      )
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


@Path("/v1.0/CPC")
class CPCService {

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def query(@QueryParam("q") q: String) = cpcSearcher.search(q)
  
  @Path("{symbol}/ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getAncestors(@PathParam("symbol") symbol: String, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      cpcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("{parentId}/children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getChildren(@PathParam("parentId") parentId: Int, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      cpcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

@Path("/v1.0/IPC")
class IPCService {
  
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def query(@QueryParam("q") q: String) = ipcSearcher.search(q)

  @Path("{symbol}/ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getAncestors(@PathParam("symbol") symbol: String, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      ipcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("{parentId}/children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getChildren(@PathParam("parentId") parentId: Int, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      ipcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

@Path("/v1.0/USPC")
class USPCService {
  
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def query(@QueryParam("q") q: String) = uspcSearcher.search(q)

  @Path("{symbol}/ancestorsAndSelf")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getAncestors(@PathParam("symbol") symbol: String, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      uspcDb.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  @Path("{parentId}/children")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getChildren(@PathParam("parentId") parentId: Int, @QueryParam("f") f: String) = {
    val fmt = getToText(f)
    database withSession { implicit session =>
      uspcDb.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
}

