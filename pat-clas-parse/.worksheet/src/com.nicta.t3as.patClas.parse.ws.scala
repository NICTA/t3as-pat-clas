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

package org.t3as.patClas.parse

import scala.xml.XML

object ws {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(115); 
  println("Welcome to the Scala worksheet");$skip(74); 
    // classnum: 3 -> 003; D3 -> D03; PLT -> PLT;
    val classnum = "D3";System.out.println("""classnum  : String = """ + $show(classnum ));$skip(23); 
    val subnum = ".10";System.out.println("""subnum  : String = """ + $show(subnum ));$skip(41); 
    val buf = new StringBuilder(20, "C");System.out.println("""buf  : StringBuilder = """ + $show(buf ));$skip(55); 
    val re = """^(PLT|D|)(\d*)~(\d*)(?:\.(\d*))?$""".r;System.out.println("""re  : scala.util.matching.Regex = """ + $show(re ));$skip(51); 
    def zeros(l: Int) = if (l > 0) "0" * l else "";System.out.println("""zeros: (l: Int)String""");$skip(62); 
    def padL(s: String, l: Int = 3) = zeros(l - s.length) + s;System.out.println("""padL: (s: String, l: Int)String""");$skip(62); 
    def padR(s: String, l: Int = 3) = s + zeros(l - s.length);System.out.println("""padR: (s: String, l: Int)String""");$skip(294); 
    
    val x = re.findPrefixMatchOf(classnum + '~' + subnum).map { mtch =>
        val re(alpha, cnum, snum, optDotNum) = mtch
        println(s"optDotNum = $optDotNum")
        "C" + alpha + zeros(3 - classnum.length) + cnum + "S" + padL(snum) + padR(Option(optDotNum).getOrElse(""))
     };System.out.println("""x  : Option[String] = """ + $show(x ))}
  
}
