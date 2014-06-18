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

import scala.collection.JavaConversions.asScalaBuffer
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.CPCUtil.ClassificationItem
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.db.CPCdb
import org.t3as.patClas.common.search.RAMIndex
import java.io.File
import org.t3as.patClas.common.search.Suggest
import org.apache.lucene.search.suggest.Lookup
import org.t3as.patClas.api.Suggestions

class TestPatClasService extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "CPCService" should "retrieve ancestorsAndSelf" in {
    val l8 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 8, "B29C31/002", "title8", "notes8"), Seq())
    val l7 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 7, "B29C31/00", "title7", "notes7"), Seq(l8))
    val l6 = TreeNode(ClassificationItem(None, -1, false, true, false, "2013-01-01", 6, "B29C31/00", "title6", "notes6"), Seq(l7))
    val l5 = TreeNode(ClassificationItem(None, -1, false, false, false, "2013-01-01", 5, "B29C", "title5", "notes5"), Seq(l6))

    // initialize singleton used by CPCService
    PatClasService.testInit(new PatClasService {
      override def indexDir(prop: String) = RAMIndex.makeTestIndex 
      override def mkCombinedSuggest(indexDir: File) = (key: String, num: Int) => Suggestions(Nil, Nil)
    } )
    
    val svc = PatClasService.service
    import svc.{cpcDb, database}
    import cpcDb.profile.simple._
    
    val srv = new CPCService
    
    {    
      val hits = srv.search("Symbol:B29C3*")
      log.debug(s"hits = $hits")
      hits.size should be(3) // matches 2 x "B29C31/00", 1 x "B29C31/002", 0 x "B29C"
    }
 
    database withSession { implicit session =>
      // Create the table(s), indices etc.
      cpcDb.cpcs.ddl.create

      // an item for top level ClassificationItems to refer to as their "parent"; forceInsert overrides the autoInc id, may not work on all databases
      val id = cpcDb.cpcs forceInsert ClassificationItem(Some(CPCdb.topLevel), CPCdb.topLevel, false, false, false, "2013-01-01", 0, "parent", "universal ancestor", "no notes")

      cpcDb.insertTree(l5, CPCdb.topLevel)

      srv.ancestorsAndSelf("B29C31/00", "xml") zip Seq(l5, l6, l7) foreach {
        case (desc, n) => {
          desc.symbol should be(n.value.symbol)
          desc.level should be(n.value.level)
          desc.classTitle should be(n.value.classTitle)
          desc.notesAndWarnings should be(n.value.notesAndWarnings)
        }
      }
    }
    
    PatClasService.close
  }
}
