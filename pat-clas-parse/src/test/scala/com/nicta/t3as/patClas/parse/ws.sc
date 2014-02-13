package org.t3as.patClas.parse

import scala.xml.XML

object ws {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
    // classnum: 3 -> 003; D3 -> D03; PLT -> PLT;
    val classnum = "D3"                           //> classnum  : String = D3
    val subnum = ".10"                            //> subnum  : String = .10
    val buf = new StringBuilder(20, "C")          //> buf  : StringBuilder = C
    val re = """^(PLT|D|)(\d*)~(\d*)(?:\.(\d*))?$""".r
                                                  //> re  : scala.util.matching.Regex = ^(PLT|D|)(\d*)~(\d*)(?:\.(\d*))?$
    def zeros(l: Int) = if (l > 0) "0" * l else ""//> zeros: (l: Int)String
    def padL(s: String, l: Int = 3) = zeros(l - s.length) + s
                                                  //> padL: (s: String, l: Int)String
    def padR(s: String, l: Int = 3) = s + zeros(l - s.length)
                                                  //> padR: (s: String, l: Int)String
    
    val x = re.findPrefixMatchOf(classnum + '~' + subnum).map { mtch =>
        val re(alpha, cnum, snum, optDotNum) = mtch
        println(s"optDotNum = $optDotNum")
        "C" + alpha + zeros(3 - classnum.length) + cnum + "S" + padL(snum) + padR(Option(optDotNum).getOrElse(""))
     }                                            //> optDotNum = 10
                                                  //| x  : Option[String] = Some(CD03S000100)
  
}
