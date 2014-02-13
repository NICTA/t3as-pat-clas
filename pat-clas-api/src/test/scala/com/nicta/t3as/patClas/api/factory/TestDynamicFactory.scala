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

package org.t3as.patClas.api.factory

import org.scalatest.{ Matchers, FlatSpec }

case class MyProduct()

class MyFactory extends Factory[Product] {
  def create = MyProduct()
  def close = {}
}

class TestDynamicFactory extends FlatSpec with Matchers {

  "DynamicFactory" should "delegate to dynamically loaded factory" in {
    val f = new DynamicFactory[Product]("org.t3as.patClas.api.factory.MyFactory")
    f.create should be (MyProduct())

    an [Exception] should be thrownBy {
      new DynamicFactory[Product]("org.t3as.patClas.api.factory.NonExistentFactory")
    }
  }
}
