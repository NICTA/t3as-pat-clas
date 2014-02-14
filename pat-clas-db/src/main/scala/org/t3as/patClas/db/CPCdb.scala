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
import org.t3as.patClas.common.CPCTypes.ClassificationItem
import org.t3as.patClas.common.TreeNode

/** Data Access Layer
  */
class CPCdb(val driver: ExtendedProfile) {
  import driver.simple._

  val log = LoggerFactory.getLogger(getClass)

  /** Slick table definition and mapping for ClassificationItem.
    */
  object CPC extends Table[ClassificationItem]("cpc") {
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

    def parent = foreignKey("cpc_parent_fk", parentId, CPC)(_.id)
    def idx = index("cpc_symbol_idx", (symbol, level), unique = true)

    def * = id.? ~ parentId ~ breakdownCode ~ allocatable ~ additionalOnly ~ dateRevised ~ level ~ symbol ~ classTitle ~ notesAndWarnings <> (ClassificationItem, ClassificationItem.unapply _)

    // While some database systems allow inserting proper values into AutoInc columns or inserting None to get a created value (e.g. H2),
    // most databases forbid this behaviour, so you have to make sure to omit these columns.
    def forInsert = parentId ~ breakdownCode ~ allocatable ~ additionalOnly ~ dateRevised ~ level ~ symbol ~ classTitle ~ notesAndWarnings <> (
      { t => ClassificationItem(None, t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9) },
      { (c: ClassificationItem) => Some((c.parentId, c.breakdownCode, c.allocatable, c.additionalOnly, c.dateRevised, c.level, c.symbol, c.classTitle, c.notesAndWarnings)) })
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
      c <- CPC if c.id === id
    } yield c

    val getByParentId = for {
      parentId <- Parameters[Int]
      c <- CPC if c.parentId === parentId
    } yield c

    val getBySymbol = for {
      symbol <- Parameters[String]
      c <- CPC if c.symbol === symbol
    } yield c

    val getBySymbolLevel = for {
      (symbol, level) <- Parameters[(String, Int)]
      c <- CPC if c.symbol === symbol && c.level === level
    } yield c
  }

  def insertTree(n: TreeNode[ClassificationItem], parentId: Int)(implicit session: Session): Unit = {
    val id = CPC.forInsert returning (CPC.id) insert n.value.copy(parentId = parentId)
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
