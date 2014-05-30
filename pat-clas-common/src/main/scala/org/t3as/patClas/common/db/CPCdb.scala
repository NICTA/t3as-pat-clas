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
import org.t3as.patClas.api.CPC.ClassificationItem
import org.t3as.patClas.common.TreeNode

/** Data Access Layer
  */
class CPCdb(val profile: JdbcProfile) {
  import profile.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for ClassificationItem. */
  class CPC(tag: Tag) extends Table[ClassificationItem](tag, "cpc") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Int]("parent_id")
    def breakdownCode = column[Boolean]("breakdown")
    def allocatable = column[Boolean]("allocatable")
    def additionalOnly = column[Boolean]("additional")
    def dateRevised = column[String]("date", O.DBType("VARCHAR(10)"))
    def level = column[Int]("level")
    def symbol = column[String]("symbol", O.DBType("VARCHAR(20)"))
    def classTitle = column[String]("title", O.DBType("CLOB"))
    def notesAndWarnings = column[String]("notesAndWarnings", O.DBType("CLOB"))

    def parent = foreignKey("cpc_parent_fk", parentId, cpcs)(_.id)
    def idx = index("cpc_symbol_idx", (symbol, level), unique = true)

    def * = (id.?, parentId, breakdownCode, allocatable, additionalOnly, dateRevised, level, symbol, classTitle, notesAndWarnings) <> (ClassificationItem.tupled, ClassificationItem.unapply)
  }
  val cpcs = TableQuery[CPC]
  
  /** Pre-compiled parameterized queries (uses JDBC PreparedStatement) */
  object compiled {
    val getById = {
      def q(id: Column[Int]) = cpcs.filter(_.id === id)
      Compiled(q _)
    }

    val getByParentId = {
      def q(parentId: Column[Int]) = cpcs.filter(_.parentId === parentId)
      Compiled(q _)
    }

    val getBySymbol = {
      def q(symbol: Column[String]) = cpcs.filter(_.symbol === symbol)
      Compiled(q _)
    }
    
    val getBySymbolLevel = {
      def q(symbol: Column[String], level: Column[Int])= cpcs.filter(c => c.symbol === symbol && c.level === level)
      Compiled(q _)
    }
    
    val insert = (cpcs returning cpcs.map(_.id)).insertInvoker
  }

  def insertTree(n: TreeNode[ClassificationItem], parentId: Int)(implicit session: Session): Unit = {
    val id = compiled.insert += n.value.copy(parentId = parentId)
    n.children foreach { n => insertTree(n, id) }
  }

  /** Get max level ClassificationItem for a given symbol.
   *  There may be multiple levels of descriptive text for the same symbol, so we get the most detailed one.
    */
  def getSymbol(symbol: String)(implicit session: Session) = {
    val list = compiled.getBySymbol(symbol).list
    if (list.isEmpty) None
    else Some(list.maxBy(_.level))
  }

  /** Get ClassificationItem for a given symbol and all its ancestors.
    */
  def getSymbolWithAncestors(symbol: String)(implicit session: Session) = {
    getSymbol(symbol).toList.flatMap(c => ancestors(c, List(c)))
  }

  /** Get ClassificationItems for a given symbol's children.
   */
  def getChildren(parentId: Int)(implicit session: Session) = {
    compiled.getByParentId(parentId).list.filter(_.id.getOrElse(parentId) != parentId).sortBy(_.symbol)
  }

  /** Prepend ancestors of c to acc and return acc. */
  @tailrec
  private def ancestors(c: ClassificationItem, acc: List[ClassificationItem])(implicit session: Session): List[ClassificationItem] = {
    if (log.isDebugEnabled()) log.debug(s"ancestors: c = $c")
    if (c.parentId == CPCdb.topLevel) acc
    else {
      val p = compiled.getById(c.parentId).first
      ancestors(p, acc.::(p))
    }
  }

}

object CPCdb {
  val topLevel = 0 // parentId value for top level ClassificationItems
}
