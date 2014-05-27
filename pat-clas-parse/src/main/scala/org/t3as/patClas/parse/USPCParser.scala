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
import org.t3as.patClas.common.TreeNode
import org.t3as.patClas.common.USPCTypes.UsClass
import scala.collection.mutable.HashMap
import scala.xml.Elem
import scala.xml.Elem
import scala.xml.Text
import scala.xml.TextBuffer
import scala.xml.NodeSeq
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.USPCTypes
import org.t3as.patClas.common.db.USPCdb

object USPCParser {
  val log = LoggerFactory.getLogger(getClass)

  def fail(msg: String) = throw new Exception(msg)
  def attr(n: Node, name: String) = n.attribute(name).map(_(0).text).getOrElse(fail("missing @" + name))

  /** Parse a US Patent Classification class XML representation.
    * @param n class Node
    * @param process do whatever processing is required
    */
  def parse(n: Node, process: UsClass => Unit) = {
    val classnum = attr(n, "classnum")
    val classXmlId = checkId(attr(n, "id"), classnum, "0") // linked to from subclass/parent/@ref
    val classTitle = n \ "title" text
    val text = excludeNodes(n, Set("title", "sclasses")).toString // XML snippet excluding named elements
    process(UsClass(None, classXmlId, "none", classnum, Some(classTitle), None, None, text))

    n \ "sclasses" \ "subclass" foreach { n =>
      val subnum = attr(n, "subnum")
      val (subnumOpt, xmlId) = {
        val xmlId = attr(n, "id")
        if (subnum.startsWith("-")) {
          val x = deriveSubnum(xmlId)
          if (x.isDefined) log.warn(s"Used xmlId = $xmlId to derive subnum ${x.get} (was subnum = $subnum)")
          (x, xmlId)
        } else (Some(subnum), checkId(xmlId, classnum, subnum))
      }
      if (subnumOpt.isDefined) {
        val parentXmlId = (n \ "parent").headOption.flatMap(_.attribute("ref").map(_(0).text)).getOrElse {
          log.warn(s"missing parent/@ref for subnum = $subnum, using parentXmlId = ${USPCdb.topXmlId}")
          USPCdb.topXmlId
        }
        // preserve XML elements (contains presentation elements and marked up refs to other classification codes) 
        val subClassTitle = n \ "sctitle" toString
        // TODO: change to None if no content does this happen other than in a few crap records?
        val subClassDescription = n \ "scdesc" toString
        val text = excludeNodes(n, Set("sctitle", "scdesc", "parent")).toString // XML snippet excluding named elements
        process(UsClass(None, xmlId, parentXmlId, classnum + '/' + subnumOpt.get, None, Some(subClassTitle), Some(subClassDescription), text))
      } else log.warn(s"Skipping subclass with subnum = $subnum and xmlId = $xmlId. Can't get a valid subclass symbol. XML = ${n.toString}")
    }

  }

  // generate xmlId from classnum & subnum to use in preference to provided value
  def checkId(xmlId: String, classnum: String, subnum: String) = {
    def zeros(l: Int) = if (l > 0) "0" * l else ""
    def padL(s: String, l: Int = 3) = zeros(l - s.length) + s
    def padR(s: String, l: Int = 3) = s + zeros(l - s.length)

    val re = """^(PLT|D|)(\d*)~(\d*)(?:\.(\d*))?$""".r // subnum can have a leading or trailing '.'
    val x = re.findPrefixMatchOf(classnum + '~' + subnum).map { mtch =>
      val re(alpha, cnum, snum, optDotNum) = mtch
      "C" + alpha + zeros(3 - classnum.length) + cnum + "S" + padL(snum) + padR(Option(optDotNum).getOrElse(""))
    }
    if (x.isDefined) {
      if (xmlId != x.get) {
        log.warn(s"overriding provided xmlId = $xmlId with ${x.get} generated from classnum = $classnum and subnum = $subnum")
        x.get
      } else xmlId
    } else {
      log.warn(s"couldn't parse classnum = $classnum and subnum = $subnum, with xmlId = $xmlId")
      xmlId
    }
  }

  // this attempt at deriving subnum from xmlId turns out to be often futile,
  // because in many cases xmlId is something like "C123S028**2"
  // and doesn't match the re and doesn't appear to contain a reasonable subnum.
  // When the re doesn't match, this creates a None.
  def deriveSubnum(xmlId: String) = {
    def trim0(s: String) = s.dropWhile((c: Char) => c == '0')

    val re = """^C(\d{3}|D\d{2}|PLT)S(\d{3})(\d*)$""".r
    re.findPrefixMatchOf(xmlId).map { mtch =>
      val re(_, subnum, dotnum) = mtch
      val sn = trim0(subnum)
      val dn = trim0(dotnum)
      if (dn.isEmpty) sn else sn + '.' + dn
    }
  }

  /** Create a filtered copy of a Node tree with Nodes with labels in excludeLabels removed */
  def excludeNodes(n: Node, excludeLabels: Set[String]) = NodeSeq.fromSeq(filter(n => !excludeLabels.contains(n.label))(n))

  /** Create a filtered copy of a Node tree.
    * Modified from Utility.trimProper.
    * Note: Element attributes are MetaData not Nodes, so are not filtered.
    * @param root root of tree
    * @param pred predicate selecting Nodes to copy to the returned tree.
    * @return modified tree
    */
  def filter(pred: Node => Boolean)(root: Node): Seq[Node] = {
    if (!pred(root)) Seq()
    else root match {
      case Elem(pre, lab, md, scp, child @ _*) =>
        Elem(pre, lab, md, scp, true, child.flatMap(filter(pred)): _*)
      case Text(s) =>
        new TextBuffer().append(s).toText // as in Utility.trimProper
      case _ =>
        root
    }
  }

}
