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

package org.t3as.patClas.examples.java;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.t3as.patClas.api.CPC;
import org.t3as.patClas.api.IPC;
import org.t3as.patClas.api.Search;
import org.t3as.patClas.api.factory.Factories;
import org.t3as.patClas.api.factory.Factory;

/**
 * Example showing how to search text associated with a classification symbol.
 */
public class SearchMain {
	static final Logger log = LoggerFactory.getLogger(SearchMain.class);

	public static void main(String[] args) throws IOException {
		cpcSearch();
		ipcSearch();
	}

	private static void cpcSearch() throws IOException {
		Factory<Search<CPC.Hit>> factory = Factories.getCPCSearchFactory();
		Search<CPC.Hit> search = factory.create();
		
		List<CPC.Hit> hits = search.search("callback mechanism");
		log.info("CPC hits = {}", hits);
		
		search.close();
		factory.close();
	}

	private static void ipcSearch() throws IOException {
		Factory<Search<IPC.Hit>> factory = Factories.getIPCSearchFactory();
		Search<IPC.Hit> search = factory.create();
		
		List<IPC.Hit> hits = search.search("callback mechanism");
		log.info("IPC hits = {}", hits);
		
		search.close();
		factory.close();
	}

}
