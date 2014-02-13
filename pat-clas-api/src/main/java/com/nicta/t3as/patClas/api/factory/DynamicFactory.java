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

import java.io.IOException;

import org.t3as.patClas.api.T3asException;

/** Factory that delegates to an instance of a dynamically loaded class.
  * @param <T> type of product
  */
public class DynamicFactory<T> implements Factory<T> {
	
	private final Factory<T> delegate;
	
	@SuppressWarnings("unchecked")
	public DynamicFactory(String fqn) {
		try {
			delegate = (Factory<T>) Class.forName(fqn).getConstructor(new Class<?>[0]).newInstance(new Object[0]);
		} catch (Exception cause) {
			throw new T3asException("Can't instantiate " + fqn, cause);
		}
	}
	
	public T create() {
		return delegate.create();
	}

	public void close() throws IOException {
		delegate.close();
	}
}
