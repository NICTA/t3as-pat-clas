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

package org.t3as.patClas.lookup.factory

import org.t3as.patClas.api.{IPC, Lookup}
import org.t3as.patClas.api.factory.Factory
import org.t3as.patClas.lookup.client.LookupWebClient
import org.t3as.patClas.common.Util, Util.{getProperty => get}

class IPCLookupFactoryImpl extends Factory[Lookup[IPC.Description]] with LCUtil {

  def create = new LookupWebClient[IPC.Description](
      get("ipc.server.url"),
      (json: String) => mapper.readValue(json)
      )

  def close = {}
}
