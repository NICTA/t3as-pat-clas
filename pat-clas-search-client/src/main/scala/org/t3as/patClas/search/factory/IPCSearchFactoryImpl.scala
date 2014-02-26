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

import org.t3as.patClas.api.{IPC, Search}
import org.t3as.patClas.api.factory.Factory
import org.t3as.patClas.lookup.factory.SCUtil
import org.t3as.patClas.search.client.SearchServiceWebClient
import org.t3as.patClas.common.Util, Util.{getProperty => get}

class IPCSearchFactoryImpl extends Factory[Search[IPC.Hit]] with SCUtil {

  def create = new SearchServiceWebClient(
    get("ipc.search.server.url"),
    (json: String) => mapper.readValue(json)
    )

  def close = {}
}
