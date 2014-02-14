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

package org.t3as.patClas.common

import scala.collection.JavaConversions._
import org.scalatest.{Matchers, FlatSpec}

class TestUtil extends FlatSpec with Matchers {

  "properties" should "load from classpath" in {
    val p = Util.properties("/util-test.properties")
    p("name") should be ("value")
  }
  
  "toText" should "concatenate all text node descendents in document order, timming whitespace" in {
    val xml = """<a> text1 <text>text  2</text>   <b>text   3<text>text 4</text>  </b>  <text>text 5</text>text    6</a>"""
    val s = Util.toText(xml)  
    s should be ("text1\ntext 2\ntext 3\ntext 4\ntext 5\ntext 6")
  }

}
