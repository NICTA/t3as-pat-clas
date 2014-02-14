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

package org.t3as.patClas.common

import org.t3as.patClas.api.CPC
import org.t3as.patClas.api.Format

object CPCTypes {

  case class Description(id: Int, symbol: String, level: Int, classTitle: String, notesAndWarnings: String) extends CPC.Description

  case class Hit(score: Float, symbol: String, level: Int, classTitleHighlights: String, notesAndWarningsHighlights: String) extends CPC.Hit
  
  /** Names of CPC fields in the Lucene index. */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    // TODO: remove ID
    val ID, Symbol, Level, ClassTitle, NotesAndWarnings = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val hitFields: Set[String] = Set(Symbol, Level)
  
  def mkHit(score: Float, f: Map[String, String], h: Map[String, String]) = Hit(score, f(Symbol), f(Level).toInt, h.getOrElse(ClassTitle, ""), h.getOrElse(NotesAndWarnings, ""))

  /** Entity class mapping to a database row representing a CPC Classification Symbol.
    * TODO: make notesAndWarnings an Option? classTitle too if it is sometimes empty.
    */
  case class ClassificationItem(id: Option[Int], parentId: Int, breakdownCode: Boolean, allocatable: Boolean, additionalOnly: Boolean,
    dateRevised: String, level: Int, symbol: String, classTitle: String, notesAndWarnings: String) {

    def toDescription(text: String => String) = Description(id.get, symbol, level, text(classTitle), text(notesAndWarnings))
  }

}
