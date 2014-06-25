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

import scala.language.postfixOps
import scala.xml.Node

import org.t3as.patClas.common.CPCUtil.ClassificationItem
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.Util.toText

object CPCParser {

   /*
    * hText (hierarchy text) is textBody of this node appended to that of all its ancestors.
    * This is indexed for searching, but not stored in Lucene or in the database.
    */    
  case class CPCNode(classificationItem: ClassificationItem, hText: String)
  
  def parse(n: Node) = n \ "classification-item" map(n => mkTree(n, ""))

  def mkTree(n: Node, hText: String): TreeNode[CPCNode] = {
    val e = cpcNode(n, hText)
    new TreeNode(e, n \ "classification-item" map (c => mkTree(c, e.hText)))
  }
  
  def cpcNode(n: Node, hText: String) = {
    def attr(n: Node, name: String) = n.attribute(name).map(_(0).text).getOrElse(throw new Exception("classification-item missing @" + name))
    
    // preserve XML elements (contains presentation elements e.g. <br/> and marked up refs to other classification codes) 
    val classTitle = n \ "class-title" toString
    val notesAndWarnings = n \ "notes-and-warnings" toString
    
    CPCNode(
      ClassificationItem(None, 0,
        attr(n, "breakdown-code") == "true", 
        attr(n, "not-allocatable") != "true",
        attr(n, "additional-only") == "true", 
        attr(n, "date-revised"), 
        attr(n, "level").toInt,
        n \ "classification-symbol" text,
        classTitle,
        notesAndWarnings
        ),
      hText + " " + toText(classTitle) + " " + toText(notesAndWarnings))
  }
}
