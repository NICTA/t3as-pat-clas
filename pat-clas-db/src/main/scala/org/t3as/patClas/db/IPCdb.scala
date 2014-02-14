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
import org.t3as.patClas.common.IPCTypes.IPCEntry
import org.t3as.patClas.common.TreeNode

/** Data Access Layer
  */
class IPCdb(val driver: ExtendedProfile) {
  import driver.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for IPCEntry.
    * removed entryType because it is always k 
    */
  object IPC extends Table[IPCEntry]("ipc") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def parentId = column[Int]("parent_id")
    def level = column[Int]("level") // XML element nesting level
    // def entryType = column[String]("entryType", O.DBType("CHAR(1)")) // always 'K' so omit
    // def ipcLevel = column[String]("ipcLevel", O.DBType("CHAR(1)")) // always 'A' so omit
    def kind = column[String]("kind", O.DBType("CHAR(1)")) // 'dot group' is 1 to 9, A, B (for 1 to 11)
    def symbol = column[String]("symbol", O.DBType("VARCHAR(20)"))
    def endSymbol = column[String]("endSymbol", O.DBType("VARCHAR(20)"), O.Nullable)
    def textBody = column[String]("textBody", O.DBType("CLOB"))

    def parent = foreignKey("ipc_parent_fk", parentId, IPC)(_.id)
    def idx = index("ipc_symbol_idx", (symbol, level, kind), unique = true)

    def * = id.? ~ parentId ~ level ~ kind ~ symbol ~ endSymbol.? ~ textBody <> (IPCEntry, IPCEntry.unapply _)

    // While some database systems allow inserting proper values into AutoInc columns or inserting None to get a created value (e.g. H2),
    // most databases forbid this behaviour, so you have to make sure to omit these columns.
    def forInsert = parentId ~ level ~ kind ~ symbol ~ endSymbol.? ~ textBody <> (
      { t => IPCEntry(None, t._1, t._2, t._3, t._4, t._5, t._6) },
      { (i: IPCEntry) => Some((i.parentId, i.level, i.kind, i.symbol, i.endSymbol, i.textBody)) })
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
      c <- IPC if c.id === id
    } yield c

    val getByParentId = for {
      parentId <- Parameters[Int]
      c <- IPC if c.parentId === parentId
    } yield c

    val getBySymbol = for {
      symbol <- Parameters[String]
      c <- IPC if c.symbol === symbol
    } yield c

    val getBySymbolLevelKind = for {
      (symbol, level, kind) <- Parameters[(String, Int, String)]
      c <- IPC if c.symbol === symbol && c.level === level && c.kind === kind
    } yield c
  }

  def insertTree(n: TreeNode[IPCEntry], parentId: Int)(implicit session: Session): Unit = {
    val id = IPC.forInsert returning (IPC.id) insert n.value.copy(parentId = parentId)
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
