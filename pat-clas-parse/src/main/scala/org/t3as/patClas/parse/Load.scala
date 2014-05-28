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

package org.t3as.patClas.parse

import java.io.File
import java.util.zip.ZipFile

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.slick.jdbc.JdbcBackend.{Database, Session}
import scala.slick.jdbc.StaticQuery
import scala.slick.jdbc.meta.MTable
import scala.util.control.NonFatal
import scala.xml.XML

import org.slf4j.LoggerFactory
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.Util
import org.t3as.patClas.common.db.{CPCdb, IPCdb, USPCdb}

import resource.managed

/** Load CPC into a database and search index.
  *
  * The input zip file is: http://www.cooperativepatentclassification.org/cpc/CPCSchemeXML201309.zip
  * (which is linked to from: http://www.cooperativepatentclassification.org/cpcSchemeAndDefinitions/Bulk.html,
  * which is probably a good place to look for updates). Manually download the zip file and pass its local path as args(0).
  */
object Load {

  val log = LoggerFactory.getLogger(getClass)

  case class Config(
    cpcZipFile: File = new File("CPCSchemeXML201312.zip"),
    ipcZipFile: File = new File("ipcr_scheme_20140101.zip"),
    uspcZipFile: File = new File("classdefsWith560fixed.zip"),
    dburl: String = "jdbc:h2:file:patClasDb",
    jdbcDriver: String = "org.h2.Driver",
    slickDriver: String = "scala.slick.driver.H2Driver",
    cpcIndexDir: File = new File("cpcIndex"),
    ipcIndexDir: File = new File("ipcIndex"),
    uspcIndexDir: File = new File("uspcIndex")
    )

  val parser = new scopt.OptionParser[Config]("load") {
    head("load", "0.x")
    val defValue = Config()

    opt[File]('c', "cpcZipFile") action { (x, c) =>
      c.copy(cpcZipFile = x)
    } text (s"path to CPC definitions in zipped XML, default ${defValue.cpcZipFile.getPath} (source http://www.cooperativepatentclassification.org/cpcSchemeAndDefinitions/Bulk.html)")
    opt[File]('d', "cpcIndexDir") action { (x, c) =>
      c.copy(cpcIndexDir = x)
    } text (s"path to CPC search index dir, default ${defValue.cpcIndexDir.getPath} (need not pre-exist)")

    opt[File]('i', "ipcZipFile") action { (x, c) =>
      c.copy(ipcZipFile = x)
    } text (s"path to IPC definitions in zipped XML, default ${defValue.ipcZipFile.getPath} (source http://www.wipo.int/classifications/ipc/en/ITsupport/)")
    opt[File]('j', "ipcIndexDir") action { (x, c) =>
      c.copy(ipcIndexDir = x)
    } text (s"path to IPC search index dir, default ${defValue.ipcIndexDir.getPath} (need not pre-exist)")

    opt[File]('u', "uspcZipFile") action { (x, c) =>
      c.copy(uspcZipFile = x)
    } text (s"path to USPC definitions in zipped XML, default ${defValue.uspcZipFile.getPath} (source https://eipweb.uspto.gov/2013/ClassDefinitions)")
    opt[File]('v', "uspcIndexDir") action { (x, c) =>
      c.copy(ipcIndexDir = x)
    } text (s"path to IPC search index dir, default ${defValue.uspcIndexDir.getPath} (need not pre-exist)")

    opt[String]("dburl") action { (x, c) =>
      c.copy(dburl = x)
    } text (s"database url, default ${defValue.dburl}")
    
    opt[String]("jdbcDriver") action { (x, c) =>
      c.copy(jdbcDriver = x)
    } text (s"JDBC driver, default ${defValue.jdbcDriver}")

    opt[String]("slickDriver") action { (x, c) =>
      c.copy(slickDriver = x)
    } text (s"Slick database driver, default ${defValue.slickDriver}")

    help("help") text ("prints this usage text")
  }

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) map { c =>
      Database.forURL(c.dburl, driver = c.jdbcDriver) withSession { implicit session =>
        StaticQuery.updateNA("CREATE USER IF NOT EXISTS READONLY PASSWORD ''").execute
        doCPC(c);
        doIPC(c);
        doUSPC(c);
      }
    }
    log.info(" ... done.")
  }

  def doCPC(c: Config)(implicit session: Session) {
    if (c.cpcZipFile.exists()) {
      log.info(s"Parsing ${c.cpcZipFile} ...")
      for (indexer <- managed(IndexerFactory.getCPCIndexer(c.cpcIndexDir))) {
        val dao = new CPCdb(Util.getObject(c.slickDriver))
        import dao.profile.simple._
        import dao.cpcs
        import org.t3as.patClas.common.CPC.ClassificationItem

        // Create the table(s), indices etc.
        if (!MTable.getTables("cpc").list.isEmpty) cpcs.ddl.drop
        cpcs.ddl.create
        StaticQuery.updateNA("""GRANT SELECT ON "cpc" TO READONLY""").execute

        // An item for top level ClassificationItems to refer to as their "parent" (to satisfy the foreign key constraint)
        // forceInsert overrides the autoInc id, works with H2 but may not work on all databases
        // With these databases some other means will be required to insert this row.
        cpcs forceInsert ClassificationItem(Some(CPCdb.topLevel), CPCdb.topLevel, false, false, false, "2013-01-01", 0, "parent", "<text>none</text>", "<text>none</text>")

        def process(t: TreeNode[ClassificationItem], parentId: Int) = {
          dao.insertTree(t, parentId) // insert tree of ClassificationItems into db
          indexer.addTree(t) // add to search index
        }

        for (zipFile <- managed(new ZipFile(c.cpcZipFile))) {
          // load section zip entries
          val sectionFileNameRE = """^cpc-scheme-[A-Z].xml$""".r
          zipFile.entries filter (e => sectionFileNameRE.findFirstIn(e.getName).isDefined) foreach { e =>
            CPCParser.parse(XML.load(zipFile.getInputStream(e))) foreach (process(_, CPCdb.topLevel))
          }

          // load subclass zip entries
          val subclassFileNameRE = """^cpc-scheme-[A-Z]\d\d[A-Z].xml$""".r
          zipFile.entries filter (e => subclassFileNameRE.findFirstIn(e.getName).isDefined) foreach { e =>
            CPCParser.parse(XML.load(zipFile.getInputStream(e))).foreach { n =>
              // each root node should be a level 5 ClassificationItem and should have already been added to the db from a section zip entry
              val c = n.value
              if (c.level != 5) throw new Exception(s"Subclass file root node not level 5: ${c}")
              val parent = dao.compiled.getBySymbolLevel(c.symbol, c.level).firstOption.getOrElse(throw new Exception(s"Subclass file root node not in db: ${c}"))
              n.children foreach (process(_, parent.id.getOrElse(throw new Exception(s"Missing id from db record: ${parent}"))))
            }
          }
        }
      }

    } else log.info(s"File ${c.cpcZipFile} not found, so skipping CPC load")
  }

  def doIPC(c: Config)(implicit session: Session) {
    if (c.ipcZipFile.exists()) {
      log.info(s"Parsing ${c.ipcZipFile} ...")
      for (indexer <- managed(IndexerFactory.getIPCIndexer(c.ipcIndexDir))) {
        val dao = new IPCdb(Util.getObject(c.slickDriver))
        import dao.profile.simple._
        import dao.ipcs
        import org.t3as.patClas.common.IPC.IPCEntry

        if (!MTable.getTables("ipc").list.isEmpty) ipcs.ddl.drop
        ipcs.ddl.create
        StaticQuery.updateNA("""GRANT SELECT ON "ipc" TO READONLY""").execute

        ipcs forceInsert IPCEntry(Some(IPCdb.topLevel), IPCdb.topLevel, 0, "", "symbol", None, "<text>none</text>")

        def process(t: TreeNode[IPCEntry], parentId: Int) = {
          dao.insertTree(t, parentId) // insert tree of IPCEntry into db
          indexer.addTree(t) // add to search index
        }

        for(zipFile <- managed(new ZipFile(c.ipcZipFile))) {
          zipFile.entries foreach { e =>
            // parent for English IPCEntries (skipping French for now)
            val parent = (XML.load(zipFile.getInputStream(e)) \ "revisionPeriod" \ "ipcEdition" \ "en" \ "staticIpc")(0)
            IPCParser.parse(parent) foreach (process(_, IPCdb.topLevel))
          }
        }

      }

    } else log.info(s"File ${c.ipcZipFile} not found, so skipping IPC load")
  }

  /**
   * The US data is dirty and can't all be successfully processed, so we need to minimize the impact by
   * catching and logging errors and carrying on.
   */
  def doUSPC(c: Config)(implicit session: Session) {
    if (c.uspcZipFile.exists()) {
      log.info(s"Parsing ${c.uspcZipFile} ...")
      for (indexer <- managed(IndexerFactory.getUSPCIndexer(c.uspcIndexDir))) {
        val dao = new USPCdb(Util.getObject(c.slickDriver))
        import dao.profile.simple._
        import dao.uspcs
        import org.t3as.patClas.common.USPC.UsClass

        if (!MTable.getTables("uspc").list.isEmpty) uspcs.ddl.drop
        uspcs.ddl.create
        StaticQuery.updateNA("""GRANT SELECT ON "uspc" TO READONLY""").execute
        
        uspcs forceInsert UsClass(Some(USPCdb.topId), USPCdb.topXmlId, USPCdb.topXmlId, "symbol", None, None, None, "<text>none</text>")

        def process(c: UsClass) = {
          try {
            uspcs += c // insert UsClass into db
            indexer.add(c) // add to search index
          } catch {
            case NonFatal(e) => log.error("Can't load: " + c, e)
          }
        }

        // XML is not well formed. E.g. parsing classdefs201308/class_106.xml without using tagsoup, we get:
        // org.xml.sax.SAXParseException; lineNumber: 1645; columnNumber: 3; The element type "graphic" must be terminated by the matching end-tag "</graphic>".
        val saxp = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl().newSAXParser()

        for (zipFile <- managed(new ZipFile(c.uspcZipFile))) {
          zipFile.entries filter (_.getName.endsWith(".xml")) foreach { ze =>
            log.info(s"Processing ${ze.getName}...")
            try {
              val root = XML.withSAXParser(saxp).load(zipFile.getInputStream(ze))
              log.debug("root.label = " + root.label) // tagsoup wraps our top level class element in html/body
              val usClass = if (root.label == "html") (root \ "body" \ "class")(0) else root
              USPCParser.parse(usClass, process)
            } catch {
              case NonFatal(e) => log.error("Can't process zip entry: " + ze.getName, e)
            }
          }
        }

      }

    } else log.info(s"File ${c.uspcZipFile} not found, so skipping USPC load")
  }

}
