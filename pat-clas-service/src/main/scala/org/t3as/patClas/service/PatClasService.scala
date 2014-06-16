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
import org.apache.lucene.store.{Directory, FSDirectory}
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.{CPCDescription, CPCHit, IPCDescription, IPCHit, Suggestions, USPCDescription, USPCHit}
import org.t3as.patClas.api.API.{Factory, LookupService, SearchService}
import org.t3as.patClas.api.javaApi.{Factory => JF}
import org.t3as.patClas.common.db.{CPCdb, IPCdb, USPCdb}
import org.t3as.patClas.common.search.{Constants, ExactSuggest, FuzzySuggest, Searcher, Suggest}
import org.t3as.patClas.common.search.Suggest.{exactSugFile, fuzzySugFile}
import javax.ws.rs.{GET, Path, Produces, QueryParam}
import javax.ws.rs.core.MediaType

object PatClasService {
  var s: Option[PatClasService] = None
  
  /** methods invoked by ServletListener configured in web.xml */
  def init = s = Some(new PatClasService)
  def close = s.map(_.close)

  def testInit(p: PatClasService) = s = Some(p)

  def service = s.getOrElse(throw new Exception("not initialised"))
}

class PatClasService {
  import org.t3as.patClas.common.Util, Util.get
  
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
  
  // TODO: make this a function returning: (key: String, num: Int) => Suggestions
  class CombinedSuggest(exact: Suggest, fuzzy: Suggest) {
    def lookup(key: String, num: Int) = {
      val x = exact.lookup(key, num)
      val n = num - x.size
      val f = if (n > 0) {
          val xs = x.toSet
          // fuzzy also gets exact matches, so filter them out
          fuzzy.lookup(key, num + 5).filter(!xs.contains(_)).take(num)
        } else List.empty
      Suggestions(x, f)
    }
  }
  
  def mkCombinedSuggest(indexDir: File) = {
    import Suggest._

    val x = new ExactSuggest
    x.load(exactSugFile(indexDir))
    
    val f = new FuzzySuggest
    f.load(fuzzySugFile(indexDir))
    
    new CombinedSuggest(x, f)
  }

  val cpcSearcher = {
    import org.t3as.patClas.common.CPCUtil._, IndexFieldName._
    new Searcher[CPCHit](textFields, unstemmedTextFields, Constants.cpcAnalyzer, hitFields, indexDir("cpc.index.path"), mkHit)
  }
  
  val cpcSuggest = mkCombinedSuggest(new File(get("cpc.index.path")))
  
  val ipcSearcher = {
    import org.t3as.patClas.common.IPCUtil._, IndexFieldName._
    new Searcher[IPCHit](textFields, unstemmedTextFields, Constants.ipcAnalyzer, hitFields, indexDir("ipc.index.path"), mkHit)
  }

  val ipcSuggest = mkCombinedSuggest(new File(get("ipc.index.path")))

  val uspcSearcher = {
    import org.t3as.patClas.common.USPCUtil._, IndexFieldName._
    new Searcher[USPCHit](textFields, unstemmedTextFields, Constants.uspcAnalyzer, hitFields, indexDir("uspc.index.path"), mkHit)
  }
 
  val uspcSuggest = mkCombinedSuggest(new File(get("uspc.index.path")))

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


// no-args ctor used by Jersey, which creates multiple instances
@Path("/v1.0/CPC")
class CPCService extends SearchService[CPCHit] with LookupService[CPCDescription] {
  val svc = PatClasService.service // singleton for things that must be shared across multiple instances; must be initialised first
  import svc._
  
  log.debug("CPCService:ctor")
  
  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String, @QueryParam("stem") stem: Boolean = true, @QueryParam("symbol") symbol: String = null) = cpcSearcher.search(q, stem, Option(symbol))
  
  @Path("suggest")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def suggest(@QueryParam("prefix") prefix: String, @QueryParam("num") num: Int) = cpcSuggest.lookup(prefix, num)
  
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
class IPCService extends SearchService[IPCHit] with LookupService[IPCDescription] {
  val svc = PatClasService.service
  import svc._
  
  log.debug("IPCService:ctor")

  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String, @QueryParam("stem") stem: Boolean = true, @QueryParam("symbol") symbol: String = null) = ipcSearcher.search(q, stem, Option(symbol))

  @Path("suggest")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def suggest(@QueryParam("prefix") prefix: String, @QueryParam("num") num: Int) = ipcSuggest.lookup(prefix, num)
  
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
class USPCService extends SearchService[USPCHit] with LookupService[USPCDescription] {
  val svc = PatClasService.service
  import svc._
  
  log.debug("USPCService:ctor")

  @Path("search")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def search(@QueryParam("q") q: String, @QueryParam("stem") stem: Boolean = true, @QueryParam("symbol") symbol: String = null) = uspcSearcher.search(q, stem, Option(symbol))

  @Path("suggest")
  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  override def suggest(@QueryParam("prefix") prefix: String, @QueryParam("num") num: Int) = uspcSuggest.lookup(prefix, num)
  
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

