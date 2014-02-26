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

package org.t3as.patClas.search.factory

import java.io.File

import org.t3as.patClas.api.{Search, USPC}
import org.t3as.patClas.api.factory.Factory
import org.t3as.patClas.common.USPCTypes
import org.t3as.patClas.common.USPCTypes.IndexFieldName._
import org.t3as.patClas.search.{Constants, SearchService}
import org.t3as.patClas.common.Util, Util.{getProperty => get}

class USPCSearchFactoryImpl extends Factory[Search[USPC.Hit]] {

  implicit val p = Util.properties("/search.properties")

  def create = {
    // if "field:query" specified leave as is, else search all text fields (accepting a match in any)
    def mkQ(q: String) = if (q.contains(":")) q
      else Seq(ClassTitle, SubClassTitle, SubClassDescription, Text).map(f => s"${f.toString}:(${q})").mkString(" || ")
    
    new SearchService[USPC.Hit](
      new File(get("uspc.index.path")),
      ClassTitle,
      USPCTypes.hitFields,
      USPCTypes.mkHit _,
      mkQ
      )
  }

  def close = {}
}
