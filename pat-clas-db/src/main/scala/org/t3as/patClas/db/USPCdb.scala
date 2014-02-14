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

package org.t3as.patClas.db

import scala.slick.driver.ExtendedProfile
import org.slf4j.LoggerFactory
import scala.annotation.tailrec
import org.t3as.patClas.common.USPCTypes.UsClass

/** Data Access Layer
  */
class USPCdb(val driver: ExtendedProfile) {
  import driver.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for ClassificationItem.
    */
  object USPC extends Table[UsClass]("uspc") {
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

    def * = id.? ~ xmlId ~ parentXmlId ~ symbol ~ classTitle.? ~ subClassTitle.? ~ subClassDescription.? ~ text <> (UsClass, UsClass.unapply _)

    // While some database systems allow inserting proper values into AutoInc columns or inserting None to get a created value (e.g. H2),
    // most databases forbid this behaviour, so you have to make sure to omit these columns.
    def forInsert = xmlId ~ parentXmlId ~ symbol ~ classTitle.? ~ subClassTitle.? ~ subClassDescription.? ~ text <> (
      { t => UsClass(None, t._1, t._2, t._3, t._4, t._5, t._6, t._7) },
      { (c: UsClass) => Some((c.xmlId, c.parentXmlId, c.symbol, c.classTitle, c.subClassTitle, c.subClassDescription, c.text)) })
  }

  /** Pre-compiled parameterized queries (uses JDBC PreparedStatement).
    *
    * In Slick 1.0.1 you can't groupBy or delete from a pre-compiled query.
    * Delete was added about a month ago, but the latest release, 2.0.0-M2, was about 3 months ago.
    * So we're out of luck for now.
    */
  object compiled {
    // make these SQL functions available in slick
    //    val length = SimpleFunction.unary[String, Int]("length")
    //    val substring = SimpleFunction.ternary[String, Int, Int, String]("substring")

    val getById = for {
      id <- Parameters[Int]
      c <- USPC if c.id === id
    } yield c

    val getByXmlId = for {
      xmlId <- Parameters[String]
      c <- USPC if c.xmlId === xmlId
    } yield c

    val getByParentXmlId = for {
      parentXmlId <- Parameters[String]
      c <- USPC if c.parentXmlId === parentXmlId
    } yield c

    val getBySymbol = for {
      symbol <- Parameters[String]
      c <- USPC if c.symbol === symbol
    } yield c

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
