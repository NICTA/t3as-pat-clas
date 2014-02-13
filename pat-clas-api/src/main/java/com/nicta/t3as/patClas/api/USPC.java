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

package org.t3as.patClas.api;

/**
 * U.S. Patent Classification types.
 */
public interface USPC {

	/** 
	 * Describe a U.S. Patent Classification symbol.
	 * Unique natural key is: symbol.
	 * Database primary key is: id.
	 */
	public interface Description {
		int id();
		
		/**
		 * For class this is USPTO XML class/@classnum,
		 * or for subclass it is class/@classnum + "/" + class/subclass/@subnum.
		 * Presence of "/" indicates subclass.
		 * This is a unique natural key.
		 */
		String symbol();
		
		/**
		 * For class this is USPTO XML class/title (which is just text),
		 * or for subclass it is empty.
		 */
		String classTitle();

		/**
		 * For class this is empty,
		 * or for subclass it is the class/subclass/sctitle element which may contain text and or markup.
		 */
		String subClassTitle();

		/**
		 * For class this is empty,
		 * for subclass it is the class/subclass/scdesc element which may contain text and or markup.
		 */
		String subClassDescription();
		
		/**
		 * For class this is the class element with all it's content except title and subclass elements.
		 * For subclass this is the subclass element with all it's content except sctitle, scdesc and parent elements.
		 */
		String text();
	}

	/** U.S. Patent Classification search result. */
	public interface Hit extends HitBase {
		String classTitleHighlights();
		String subClassTitleHighlights();
		String subClassDescriptionHighlights();
		String textHighlights();
	}
}
