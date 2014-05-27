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

import scala.xml.Node

import org.t3as.patClas.common.CPCTypes.ClassificationItem
import org.t3as.patClas.common.TreeNode

object CPCParser {

  def parse(n: Node) = n \ "classification-item" map(n => mkTree(n))

  def mkTree(n: Node): TreeNode[ClassificationItem] = new TreeNode(
    classificationItem(n),
    n \ "classification-item" map (c => mkTree(c)))

  def classificationItem(n: Node) = {
    def attr(n: Node, name: String) = n.attribute(name).map(_(0).text).getOrElse(throw new Exception("classification-item missing @" + name))
    
    ClassificationItem(None, 0,
        attr(n, "breakdown-code") == "true", 
        attr(n, "not-allocatable") != "true",
        attr(n, "additional-only") == "true", 
        attr(n, "date-revised"), 
        attr(n, "level").toInt,
        n \ "classification-symbol" text,
        // preserve XML elements (contains presentation elements e.g. <br/> and marked up refs to other classification codes) 
        n \ "class-title" toString,
        n \ "notes-and-warnings" toString)
  }
}
