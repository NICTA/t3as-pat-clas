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

class TestAPI extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "API.ltrim" should "left trim" in {
    for ((in, out) <- Seq(("", ""), ("0", ""), ("00", ""), ("01", "1"), ("001", "1"), ("010", "10"), ("00100", "100"))) {
      API.ltrim(in, '0') should be (out)
    }
  }

  "API.rtrim" should "right trim" in {
    for ((in, out) <- Seq(("", ""), ("0", ""), ("00", ""), ("10", "1"), ("100", "1"), ("010", "01"), ("00100", "001"))) {
      API.rtrim(in, '0') should be (out)
    }
  }
}