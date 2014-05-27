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
import org.t3as.patClas.common.IPCTypes.IPCEntry
import org.t3as.patClas.common.TreeNode

/** Data Access Layer
  */
class IPCdb(val profile: JdbcProfile) {
  import profile.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for IPCEntry.
    * removed entryType because it is always k 
    */
  class IPC(tag: Tag) extends Table[IPCEntry](tag, "ipc") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Int]("parent_id")
    def level = column[Int]("level") // XML element nesting level
    // def entryType = column[String]("entryType", O.DBType("CHAR(1)")) // always 'K' so omit
    // def ipcLevel = column[String]("ipcLevel", O.DBType("CHAR(1)")) // always 'A' so omit
    def kind = column[String]("kind", O.DBType("CHAR(1)")) // 'dot group' is 1 to 9, A, B (for 1 to 11)
    def symbol = column[String]("symbol", O.DBType("VARCHAR(20)"))
    def endSymbol = column[String]("endSymbol", O.DBType("VARCHAR(20)"), O.Nullable)
    def textBody = column[String]("textBody", O.DBType("CLOB"))

    def parent = foreignKey("ipc_parent_fk", parentId, ipcs)(_.id)
    def idx = index("ipc_symbol_idx", (symbol, level, kind), unique = true)

    def * = (id.?, parentId, level, kind, symbol, endSymbol.?, textBody) <> (IPCEntry.tupled, IPCEntry.unapply)
  }
  val ipcs = TableQuery[IPC]
  
  /** Pre-compiled parameterized queries (uses JDBC PreparedStatement). */
  object compiled {
    val getById = {
      def q(id: Column[Int]) = ipcs.filter(_.id === id)
      Compiled(q _)
    }

    val getByParentId = {
      def q(parentId: Column[Int]) = ipcs.filter(_.parentId === parentId)
      Compiled(q _)
    }

    val getBySymbol = {
      def q(symbol: Column[String]) = ipcs.filter(_.symbol === symbol)
      Compiled(q _)
    }

    val getBySymbolLevelKind = {
      def q(symbol: Column[String], level: Column[Int], kind: Column[String])= ipcs.filter(c => c.symbol === symbol && c.level === level && c.kind === kind)
      Compiled(q _)
    }
    
    val insert = (ipcs returning ipcs.map(_.id)).insertInvoker
  }

  def insertTree(n: TreeNode[IPCEntry], parentId: Int)(implicit session: Session): Unit = {
    val id = compiled.insert += n.value.copy(parentId = parentId)
    n.children foreach { n => insertTree(n, id) }
  }

  /** Get max level IPCEntry for a given symbol.
    * There may be multiple levels of descriptive text for the same symbol, so we get the one
    * with the highest level and break ties on most detailed kind.
    */
  def getSymbol(symbol: String)(implicit session: Session) = {
    val list = compiled.getBySymbol(symbol).list
    if (list.isEmpty) None
    else Some(list.maxBy(rank(_)))
  }
  
  /** used to decide most detailed entry for symbol */
  private def rank(i: IPCEntry) = i.level * 100 + "stcugm123456789ABdinl".indexOf(i.kind.charAt(0))
  
  /** Get IPCEntry for a given symbol and all its ancestors.
    */
  def getSymbolWithAncestors(symbol: String)(implicit session: Session) = {
    getSymbol(symbol).toList.flatMap(c => ancestors(c, List(c)))
  }

  /** Get IPCEntry's for a given symbol's children.
   */
  def getChildren(parentId: Int)(implicit session: Session) = {
    compiled.getByParentId(parentId).list.filter(_.id.getOrElse(parentId) != parentId).sortBy(_.symbol)
  }

  /** Prepend ancestors of c to acc and return acc.
    */
  @tailrec
  private def ancestors(c: IPCEntry, acc: List[IPCEntry])(implicit session: Session): List[IPCEntry] = {
    if (log.isDebugEnabled()) log.debug(s"ancestors: c = $c")
    if (c.parentId == IPCdb.topLevel) acc
    else {
      val p = compiled.getById(c.parentId).first
      ancestors(p, acc.::(p))
    }
  }

}

object IPCdb {
  val topLevel = 0 // parentId value for top level IPCEntry's
}
