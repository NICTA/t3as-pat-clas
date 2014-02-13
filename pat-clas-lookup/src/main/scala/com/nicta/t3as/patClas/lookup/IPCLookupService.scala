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

import scala.collection.JavaConversions.seqAsJavaList
import scala.slick.session.Database

import org.t3as.patClas.api.{IPC, Format, Lookup}
import org.t3as.patClas.common.IPCTypes
import org.t3as.patClas.common.IPCTypes.IPCEntry
import org.t3as.patClas.common.Util
import org.t3as.patClas.db.IPCdb

class IPCLookupService(database: Database, dao: IPCdb) extends Lookup[IPC.Description] {

  import dao.driver.simple.Database.threadLocalSession
  
  override def getAncestors(symbol: String, f: Format) = {
    val fmt = Util.getToText(f)
    database withSession {
      dao.getSymbolWithAncestors(symbol).map(_.toDescription(fmt))
    }
  }

  override def getChildren(parentId: Int, f: Format) = {
    val fmt = Util.getToText(f)
    database withSession {
      dao.getChildren(parentId).map(_.toDescription(fmt))
    }
  } 
  
  override def close = {}
}
