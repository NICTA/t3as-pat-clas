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

package org.t3as.patClas.api.factory;

import org.t3as.patClas.api.CPC;
import org.t3as.patClas.api.IPC;
import org.t3as.patClas.api.Lookup;
import org.t3as.patClas.api.Search;
import org.t3as.patClas.api.USPC;

public class Factories {
	private static <T> Factory<T> getFactory(String fqn) {
		return new DynamicFactory<T>(fqn);
	}

	public static Factory<Lookup<CPC.Description>> getCPCLookupFactory() {
		return getFactory("org.t3as.patClas.lookup.factory.CPCLookupFactoryImpl");
	}

	public static Factory<Lookup<IPC.Description>> getIPCLookupFactory() {
		return getFactory("org.t3as.patClas.lookup.factory.IPCLookupFactoryImpl");
	}

	public static Factory<Lookup<USPC.Description>> getUSPCLookupFactory() {
		return getFactory("org.t3as.patClas.lookup.factory.USPCLookupFactoryImpl");
	}

	public static Factory<Search<CPC.Hit>> getCPCSearchFactory() {
		return getFactory("org.t3as.patClas.search.factory.CPCSearchFactoryImpl");
	}

	public static Factory<Search<USPC.Hit>> getUSPCSearchFactory() {
		return getFactory("org.t3as.patClas.search.factory.USPCSearchFactoryImpl");
	}

	public static Factory<Search<IPC.Hit>> getIPCSearchFactory() {
		return getFactory("org.t3as.patClas.search.factory.IPCSearchFactoryImpl");
	}

}
