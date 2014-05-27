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

import org.t3as.patClas.common.IPCTypes.IPCEntry
import org.t3as.patClas.common.TreeNode

object IPCParser {

  def parse(n: Node) = n \ "ipcEntry" map (n => mkTree(n, 0))

  def mkTree(n: Node, level: Int): TreeNode[IPCEntry] = new TreeNode(
    ipcEntry(n, level),
    n \ "ipcEntry" map (n => mkTree(n, level + 1)))

  def ipcEntry(n: Node, level: Int): IPCEntry = {
    def attrOption(n: Node, name: String) = n.attribute(name).map(_(0).text)
    def attr(n: Node, name: String) = attrOption(n, name).getOrElse(throw new Exception("ipcEntry missing @" + name))

    IPCEntry(None, 0, level,
      // attr(n, "entryType"), always K, so omit
      attr(n, "kind"),
      attr(n, "symbol"),
      attrOption(n, "endSymbol"),
      // preserve XML elements (contains presentation elements e.g. <emdash/> and marked up refs to other classification codes) 
      n \ "textBody" toString)
  }

}
