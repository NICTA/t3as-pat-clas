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

package org.t3as.patClas.lookup

import scala.collection.JavaConversions.asScalaBuffer
import scala.slick.driver.H2Driver.simple._, Database.threadLocalSession

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import org.t3as.patClas.api.Format
import org.t3as.patClas.common.{CPCTypes, TreeNode}, CPCTypes.ClassificationItem
import org.t3as.patClas.db.CPCdb
import org.t3as.patClas.lookup.factory.CPCLookupFactoryImpl

class TestCPCLookupService extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "LookupService" should "retrieve ancestors" in {

    val l8 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 8, "B29C31/002", "title8", "notes8"), Seq())
    val l7 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 7, "B29C31/00", "title7", "notes7"), Seq(l8))
    val l6 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 6, "B29C31/00", "title6", "notes6"), Seq(l7))
    val l5 = TreeNode(ClassificationItem(None, -1, false, false, false, "2013-01-01", 5, "B29C", "title5", "notes5"), Seq(l6))

    val factory = new CPCLookupFactoryImpl
    factory.database withSession {
      
      // Create the table(s), indices etc.
      import factory.dao.CPC
      CPC.ddl.create
      // an item for top level ClassificationItems to refer to as their "parent"
      CPC insert new ClassificationItem(Some(CPCdb.topLevel), CPCdb.topLevel, false, false, false, "2013-01-01", 0, "parent", "universal ancestor", "no notes")

      factory.dao.insertTree(l5, CPCdb.topLevel)

      val lookup = factory.create
      lookup.getAncestors("B29C31/00", Format.XML) zip Seq(l5, l6, l7) foreach {
        case (desc, n) => {
          desc.symbol should be(n.value.symbol)
          desc.level should be(n.value.level)
          desc.classTitle should be(n.value.classTitle)
          desc.notesAndWarnings should be(n.value.notesAndWarnings)
        }
      }
      
      lookup.close()
    }

    factory.close()
  }

}
