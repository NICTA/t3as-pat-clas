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
import org.t3as.patClas.common.Util, Util.get
import org.t3as.patClas.api.{CPC, IPC, USPC, API}, API.{SearchService, LookupService, Factory}
import org.t3as.patClas.common.search.{Constants, Searcher, RAMIndex}
import org.apache.lucene.store.{Directory, FSDirectory}

object PatClasService {
  var s: Option[PatClasService] = None
  
  /** methods invoked by ServletListener configured in web.xml */
  def init = s = Some(new PatClasService)
  def close = s.map(_.close)

  def testInit(p: PatClasService) = s = Some(p)

  def service = s.getOrElse(throw new Exception("not initialised"))
}

class PatClasService {
  val log = LoggerFactory.getLogger(getClass)
  
  log.debug("PatClasService:ctor")

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
  
  // tests can override with a RAMDirectory
  def indexDir(prop: String): Directory = FSDirectory.open(new File(get(prop)))
  
  val cpcSearcher = {
    import CPC._, IndexFieldName._
    new Searcher[Hit](textFields, Constants.cpcAnalyzer, hitFields, indexDir("cpc.index.path"), mkHit)
  }

  val ipcSearcher = {
    import IPC._, IndexFieldName._
    new Searcher[Hit](textFields, Constants.ipcAnalyzer, hitFields, indexDir("ipc.index.path"), mkHit)
  }

  val uspcSearcher = {
    import USPC._, IndexFieldName._
    new Searcher[Hit](textFields, Constants.uspcAnalyzer, hitFields, indexDir("uspc.index.path"), mkHit)
  }
 
  def close = {
    log.info("Closing datasource and search indices")
    datasource.close
    cpcSearcher.close
    ipcSearcher.close
    uspcSearcher.close
  }
  
  // for local (in-process) use from Scala
  def factory = new Factory {
    val cpc = new CPCService
    val ipc = new IPCService
    val uspc = new USPCService
    override def close = PatClasService.close
  }
  
  // for local (in-process) use from Java
  import org.t3as.patClas.api.javaApi.{Factory => JF}
  def toJavaApi = new JF(factory)
}


// no-args ctor used by Jersey, not sure whether it could create multiple instances, but its OK if it does
@Path("/v1.0/CPC")
class CPCService extends SearchService[CPC.Hit] with LookupService[CPC.Description] {
  val svc = PatClasService.service // singleton for things that must be shared across multiple instances; must be initialised first
  import svc._
  
  log.debug("CPCService:ctor")
  
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
  val svc = PatClasService.service
  import svc._
  
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
  val svc = PatClasService.service
  import svc._
  
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

