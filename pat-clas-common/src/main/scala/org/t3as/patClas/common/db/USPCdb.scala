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

package org.t3as.patClas.common.db

import scala.slick.driver.JdbcProfile
import org.slf4j.LoggerFactory
import scala.annotation.tailrec
import org.t3as.patClas.common.USPCUtil.UsClass

/** Data Access Layer
  */
class USPCdb(val profile: JdbcProfile) {
  import profile.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for UsClass */
  class USPC(tag: Tag) extends Table[UsClass](tag, "uspc") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def xmlId = column[String]("xmlId", O.DBType("VARCHAR(12)"))
    def parentXmlId = column[String]("parentXmlId", O.DBType("VARCHAR(12)"))
    def symbol = column[String]("symbol", O.DBType("VARCHAR(11)"))
    def classTitle = column[String]("classTitle", O.DBType("CLOB"), O.Nullable)
    def subClassTitle = column[String]("subClassTitle", O.DBType("CLOB"), O.Nullable)
    def subClassDescription = column[String]("subClassDescription", O.DBType("CLOB"), O.Nullable)
    def text = column[String]("text", O.DBType("CLOB"))

    // def parent = foreignKey("uspc_parent_fk", parentXmlId, USPC)(_.xmlId) // insert order does not ensure parent exists before child
    def xmlIdIdx = index("uspc_xmlId_idx", xmlId, unique = true)
    def parentXmlIdIdx = index("uspc_parentXmlIdIdx", parentXmlId)
    def symbolIdx = index("uspc_symbol_idx", symbol, unique = true)

    def * = (id.?, xmlId, parentXmlId, symbol, classTitle.?, subClassTitle.?, subClassDescription.?, text) <> (UsClass.tupled, UsClass.unapply)
  }
  val uspcs = TableQuery[USPC]

  /** Pre-compiled parameterized queries (uses JDBC PreparedStatement). */
  object compiled {
    val getById = {
      def q(id: Column[Int]) = uspcs.filter(_.id === id)
      Compiled(q _)
    }

    val getByXmlId = {
      def q(xmlId: Column[String]) = uspcs.filter(_.xmlId === xmlId)
      Compiled(q _)
    }

    val getByParentXmlId = {
      def q(parentXmlId: Column[String]) = uspcs.filter(_.parentXmlId === parentXmlId)
      Compiled(q _)
    }

    val getBySymbol = {
      def q(symbol: Column[String]) = uspcs.filter(_.symbol === symbol)
      Compiled(q _)
    }

    val insert = (uspcs returning uspcs.map(_.id)).insertInvoker
  }

  /** Get UsClass for a given symbol and all its ancestors.
    */
  def getSymbolWithAncestors(symbol: String)(implicit session: Session) = {
    compiled.getBySymbol(symbol).list.flatMap(c => ancestors(c, List(c)))
  }

  /** Get ClassificationItems for a given symbol's children.
   */
  def getChildren(parentId: Int)(implicit session: Session) = {
    compiled.getById(parentId).list.flatMap(p => compiled.getByParentXmlId(p.xmlId).list.filter(_.xmlId != p.xmlId).sortBy(_.symbol))
  }

  /** Prepend ancestors of c to acc and return acc.
    */
  @tailrec
  private def ancestors(c: UsClass, acc: List[UsClass])(implicit session: Session): List[UsClass] = {
    if (log.isDebugEnabled()) log.debug(s"ancestors: c = $c")
    if (c.parentXmlId == USPCdb.topXmlId) acc
    else {
      val pOpt = compiled.getByXmlId(c.parentXmlId).firstOption
      // TODO: US data is dirty - many parent's don't exist - most of these have a missing digit
      // e.g. C106S00150, if a '0' is inserted before the 2nd to last char it might correct many
      // but for now we just don't look any higher
      if (!pOpt.isDefined) acc
      else {
        val p = pOpt.get
        ancestors(p, acc.::(p))
      }
    }
  }

}

object USPCdb {
  val topId = 0 // id for parent of top level USClasses
  val topXmlId = "none" // parentXmlId value for top level USClasses
}
