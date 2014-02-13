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

import java.io.Closeable;
import java.util.List;

/** Lookup a patent classification symbol. */
public interface Lookup<Description> extends Closeable {
	
	// TODO: add? Description get(String symbol, int level);
	
	/**
	 * Get text associated with the given symbol.
	 * 
	 * The symbol may occur at multiple levels in the hierarchy.
	 * It is located at the max level at which it occurs and this element is returned as the last item in the list.
	 * It is preceded by all its ancestors back to the root of the tree, which is returned as the first element in the list.
	 * @param symbol
	 * @param format
	 * @return descriptions of symbol and its ancestors
	 */
	List<Description> getAncestors(String symbol, Format format);

	/**
	 * Get the children of the given symbol.
	 * @param parentId
	 * @return descriptions of children
	 */
	List<Description> getChildren(int parentId, Format format);
}
