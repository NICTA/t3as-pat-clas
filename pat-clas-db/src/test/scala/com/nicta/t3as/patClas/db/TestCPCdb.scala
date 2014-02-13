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

import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

import scala.slick.driver.H2Driver, H2Driver.simple._, Database.threadLocalSession

import org.slf4j.LoggerFactory

import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.CPCTypes.ClassificationItem


class TestCPCdb extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "CPCdb" should "load and lookup" in {
    // Connect to the database and execute the following block within a session
    Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withSession {
      // The session is never named explicitly. It is bound to the current
      // thread as the threadLocalSession that we imported

      val dao = new CPCdb(H2Driver)

      import dao.CPC

      // Create the table(s), indices etc.
      CPC.ddl.create

      // an item for top level ClassificationItems to refer to as their "parent"
      CPC insert new ClassificationItem(Some(CPCdb.topLevel), CPCdb.topLevel, false, false, false, "2013-01-01", 0, "parent", "universal ancestor", "no notes")
      
      val l8 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 8, "B29C31/002", "title8", "notes8"), Seq())
      val l7 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 7, "B29C31/00",  "title7", "notes7"), Seq(l8))
      val l6 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 6, "B29C31/00",  "title6", "notes6"), Seq(l7))
      val l5 = TreeNode(ClassificationItem(None, -1, false, false, false, "2013-01-01", 5, "B29C",      "title5", "notes5"), Seq(l6))
      dao.insertTree(l5, CPCdb.topLevel)
          
      a [java.sql.SQLException] should be thrownBy {
          // duplicate (symbol, level) value and there is a unique index
          CPC.forInsert insert new ClassificationItem(None, CPCdb.topLevel, false, false, false, "2013-01-01", 5, "B29C", "title5", "notes5")
      }
      
      val list = dao.compiled.getBySymbol("B29C31/00").list
      list.size should be(2)
      list.exists(c => c.symbol == "B29C31/00" && c.level == 6) should be(true)
      list.exists(c => c.symbol == "B29C31/00" && c.level == 7) should be(true)

      val l7list = dao.compiled.getBySymbolLevel("B29C31/00", 7).list
      l7list.size should be(1)
      l7list.exists(c => c.symbol == "B29C31/00" && c.level == 7) should be(true)

      dao.getSymbolWithAncestors("B29C31/00") zip Seq((1, 0, 5, "B29C"), (2, 1, 6, "B29C31/00"), (3, 2, 7, "B29C31/00")) foreach {
        case (c, (id, parentId, level, symbol)) => {
          c.id should be (Some(id))         // autoInc id starts at 1
          c.parentId should be (parentId)   // insertTree fixes the parentId (-1 above is ignored)
          c.level should be (level)
          c.symbol should be (symbol)
        }
      }
    }

  }

}
