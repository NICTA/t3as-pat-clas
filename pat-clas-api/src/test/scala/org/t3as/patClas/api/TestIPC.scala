/*
    Copyright 2013, 2014 NICTA
    
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

package org.t3as.patClas.api

import org.scalatest.Matchers
import org.slf4j.LoggerFactory
import org.scalatest.FlatSpec

class TestIPC extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "IPC.toCpcFormat" should "convert to CPC style format" in {
    for ((in, out) <- Seq(("A01B0012987000", "A01B12/987"), ("A01B0012986000", "A01B12/986"), ("A01B0012000000", "A01B12"))) {
      IPC.toCpcFormat(in) should be (out)
    }
  }
}