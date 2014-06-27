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
import org.t3as.patClas.common.IPCUtil.IPCEntry
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.Util.toText

object IPCParser {
  
   /*
    * hText (hierarchy text) is textBody of this node appended to that of all its ancestors.
    * This is indexed for searching, but not stored in Lucene or in the database.
    */    
  case class IPCNode(ipcEntry: IPCEntry, hText: String)

  def parse(n: Node) = n \ "ipcEntry" map (n => mkTree(n, 0, ""))

  def mkTree(n: Node, level: Int, hText: String): TreeNode[IPCNode] = {
    val e = ipcNode(n, level, hText);
    new TreeNode(e, n \ "ipcEntry" map (n => mkTree(n, level + 1, e.hText)))
  }

  def ipcNode(n: Node, level: Int, hText: String) = {
    def attrOption(n: Node, name: String) = n.attribute(name).map(_(0).text)
    def attr(n: Node, name: String) = attrOption(n, name).getOrElse(throw new Exception("ipcEntry missing @" + name))

    // preserve XML elements (contains presentation elements e.g. <emdash/> and marked up refs to other classification codes) 
    val textBody = n \ "textBody" toString
    
    IPCNode(IPCEntry(None, 0, level,
      // attr(n, "entryType"), always K, so omit
      attr(n, "kind"),
      attr(n, "symbol"),
      attrOption(n, "endSymbol"),
      textBody
      ),
      hText + " " + toText(textBody))
  }

}
