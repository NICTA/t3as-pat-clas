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
import org.t3as.patClas.api.Lookup;
import org.t3as.patClas.api.Format;
import org.t3as.patClas.api.factory.Factories;
import org.t3as.patClas.api.factory.Factory;

/**
 * Example showing how to search text associated with a classification symbol.
 */
public class LookupMain {
	static final Logger log = LoggerFactory.getLogger(LookupMain.class);

	public static void main(String[] args) throws IOException {
		cpcLookup();
		ipcLookup();
	}

	private static void cpcLookup() throws IOException {
		Factory<Lookup<CPC.Description>> factory = Factories.getCPCLookupFactory();
		Lookup<CPC.Description> lookup = factory.create();
		
		List<CPC.Description> xmlSnippets = lookup.getAncestors("H05K2203/0743", Format.XML);
		log.info("CPC xmlSnippets = {}", xmlSnippets);
		
		List<CPC.Description> text = lookup.getChildren(810, Format.PLAIN);
		log.info("CPC text = {}", text);
		
		lookup.close();
		factory.close();
	}

	private static void ipcLookup() throws IOException {
		Factory<Lookup<IPC.Description>> factory = Factories.getIPCLookupFactory();
		Lookup<IPC.Description> lookup = factory.create();
		
		List<IPC.Description> xmlSnippets = lookup.getAncestors("A01K0041060000", Format.PLAIN);
		log.info("IPC xmlSnippets = {}", xmlSnippets);
		
		List<IPC.Description> text = lookup.getChildren(200, Format.XML);
		log.info("IPC text = {}", text);
		
		lookup.close();
		factory.close();
	}
}
