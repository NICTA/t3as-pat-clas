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

package org.t3as.patClas.common

import java.util.Properties

import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.language.implicitConversions
import scala.xml.{Utility, XML}

import org.slf4j.LoggerFactory
import org.t3as.patClas.api.{CPCDescription, CPCHit, HitSymbol, IPCDescription, IPCHit, USPCDescription, USPCHit}

object Util {
  val log = LoggerFactory.getLogger(getClass)
  
  def properties(path: String) = {
    val p = new Properties
    val is = getClass.getResourceAsStream(path)
    try p.load(is)
    finally is.close
    log.info(s"Util.properties: loaded ${propertiesAsScalaMap(p)}")
    p
  }

  /** prefer system property then properties file */
  def get(name: String)(implicit props: Properties) = {
    val v = sys.props.getOrElse(name, props.getProperty(name))
    log.info(s"Util.get: name = ${name}, sys value = ${sys.props.get(name)}, returned value = $v")
    v
  }

  /** Concatenate all text node descendants */
  def toText(xml: String) = {
    if (xml.isEmpty) ""
    else (for {
      n <- Utility.trim(XML.loadString(xml)).descendant
      if n.isAtom
    } yield n.text) mkString("\n")
  }
  
  /** trim leading c's from s */
  def ltrim(s: String, c: Char) = {
    val i = s.indexWhere(_ != c)
    if (i == -1) "" else s.substring(i)
  }
  
  /** trim trailing c's from s */
  def rtrim(s: String, c: Char) = {
    val i = s.lastIndexWhere(_ != c)
    s.substring(0, i + 1)
  }

  /**
   * Get a Scala singleton Object.
   * @param fqn object's fully qualified name
   * @return object as type T
   */
  def getObject[T](fqn: String): T = {
    val m = scala.reflect.runtime.universe.runtimeMirror(getClass.getClassLoader)
    m.reflectModule(m.staticModule(fqn)).instance.asInstanceOf[T]
  }
}

object CPCUtil {
  /** Names of CPC fields in the Lucene index. */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    val Symbol, Level, ClassTitle, ClassTitleUnstemmed, NotesAndWarnings, NotesAndWarningsUnstemmed, HText, HTextUnstemmed = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val textFields: List[String] = List(ClassTitle, NotesAndWarnings)
  val unstemmedTextFields: List[String] = List(ClassTitleUnstemmed, NotesAndWarningsUnstemmed) // in pref order for suggester

  val analyzerTextFields: List[String] = HText :: textFields
  val analyzerUnstemmedTextFields: List[String] = HTextUnstemmed :: unstemmedTextFields
  
  val hitFields: List[String] = List(Symbol, Level) // fields to retrieve from search hits, in addition to textFields/unstemmedTextFields
  
  def mkHit(score: Float, stem: Boolean, f: Map[String, String], h: Map[String, String]) = {
    val getH =
      if (stem) (s: String, u: String) => h.getOrElse(s, h.getOrElse(u, f.getOrElse(s, "")))
      else (s: String, u: String) => h.getOrElse(u, h.getOrElse(s, f.getOrElse(u, "")))
    
    val s = f(Symbol).toUpperCase
    CPCHit(score,
      HitSymbol(s, s),
      f(Level).toInt, 
      getH(ClassTitle, ClassTitleUnstemmed), 
      getH(NotesAndWarnings, NotesAndWarningsUnstemmed)
    )
  }

  /** Entity class mapping to a database row representing a CPC Classification Symbol.
    * TODO: make notesAndWarnings an Option? classTitle too if it is sometimes empty.
    */
  case class ClassificationItem(id: Option[Int], parentId: Int, breakdownCode: Boolean, allocatable: Boolean, additionalOnly: Boolean,
    dateRevised: String, level: Int, symbol: String, classTitle: String, notesAndWarnings: String) {

    def toDescription(text: String => String) = CPCDescription(id.get, symbol, level, text(classTitle), text(notesAndWarnings))
  }  
}

object IPCUtil {
  /**
   * Names of IPC fields in the Lucene index.
   * 
   * IPC symbols in the source data have format:
   *   A99AZMMMGGGGGZ (Z = zero padded, ZMMM = 4 digit left padded main group, GGGGGZ = 6 digit right padded sub group)
   * which is harder to read and inconsistent with the format of ref's to IPCs in the CPC:
   *   A99AMM/GG.
   * We display and allow searching in this second format stored in the Symbol field; but also store the original format in SymbolRaw
   * because that format is used in database lookups.
   */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    val Symbol, SymbolRaw, Level, Kind, TextBody, TextBodyUnstemmed, HText, HTextUnstemmed = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val textFields: List[String] = List(TextBody)
  val unstemmedTextFields: List[String] = List(TextBodyUnstemmed)

  val analyzerTextFields: List[String] = HText :: textFields
  val analyzerUnstemmedTextFields: List[String] = HTextUnstemmed :: unstemmedTextFields
  
  val hitFields: List[String] = List(Symbol, SymbolRaw, Level, Kind)
  
  def mkHit(score: Float, stem: Boolean, f: Map[String, String], h: Map[String, String]) = {
    val getH =
      if (stem) (s: String, u: String) => h.getOrElse(s, h.getOrElse(u, f.getOrElse(s, "")))
      else (s: String, u: String) => h.getOrElse(u, h.getOrElse(s, f.getOrElse(u, "")))
    
      IPCHit(score, 
        HitSymbol(f(SymbolRaw), f(Symbol).toUpperCase), 
        f(Level).toInt, 
        f(Kind), 
        getH(TextBody, TextBodyUnstemmed)
      )
  }
  
  /** Entity class mapping to a database row representing a IPCEntry */
  case class IPCEntry(id: Option[Int], parentId: Int, level: Int, kind: String, symbol: String, endSymbol: Option[String], textBody: String) {
    def toDescription(text: String => String) = IPCDescription(id.get, symbol, level, kind, text(textBody))
  }

  private val re = """(\p{Upper}\p{Digit}{2}\p{Upper})(\p{Digit}{4})(\p{Digit}{6})""".r
  
  def toCpcFormat(s: String) = {
    import Util.{ltrim, rtrim}
    
    if (s.length != 14) s
    else {
      s match {
        case re(sectionClassSubclass, mainGroup, subGroup) => {
          val sg = rtrim(subGroup, '0')
          sectionClassSubclass + ltrim(mainGroup, '0') + (sg.length match {
            case 0 => ""
            case 1 => "/" + sg + "0"
            case _ => "/" + sg
          })
        }
        case _ => s
      }
    }
  } 
  
  // Like Util.toText (which concatenates descendant text nodes), we do that but also take
  // symbols from sref/@ref attributes so that references to symbols are not lost.
  // The symbols are reformated with toCpcFormat(). 
  def ipcToText(xml: String) = {
    if (xml.isEmpty) ""
    else {
      val multiSpace = """\s+""".r
      XML.loadString(xml) flatMap (_.descendant_or_self) flatMap { n =>
        if (n.isAtom) {
          val s = multiSpace replaceAllIn(n.text.trim, { _ => " " })
          if (s.isEmpty()) None else Some(s)
        }
        else if ("sref" == n.label) Some(toCpcFormat((n \ "@ref").toString))
        else None
      } mkString("\n")
    }
  }
}

object USPCUtil {
  /** Names of USPC fields in the Lucene index. */
  object IndexFieldName extends Enumeration {
    type IndexFieldName = Value
    val Symbol, ClassTitle, ClassTitleUnstemmed, SubClassTitle, SubClassTitleUnstemmed, SubClassDescription, SubClassDescriptionUnstemmed, Text, TextUnstemmed, HText, HTextUnstemmed = Value

    implicit def convert(f: IndexFieldName) = f.toString
  }
  import IndexFieldName._
  
  val textFields: List[String] = List(ClassTitle, SubClassTitle, SubClassDescription, Text)
  val unstemmedTextFields: List[String] = List(TextUnstemmed, SubClassDescriptionUnstemmed, SubClassTitleUnstemmed, ClassTitleUnstemmed) // in pref order for suggester

  val analyzerTextFields: List[String] = HText :: textFields
  val analyzerUnstemmedTextFields: List[String] = HTextUnstemmed :: unstemmedTextFields
  
  val hitFields: List[String] = List(Symbol)
  
  def mkHit(score: Float, stem: Boolean, f: Map[String, String], h: Map[String, String]) = {
    val getH =
      if (stem) (s: String, u: String) => h.getOrElse(s, h.getOrElse(u, f.getOrElse(s, ""))) // try stemmed highlights, then unstemmed highlights, then stemmed without highlights
      else      (s: String, u: String) => h.getOrElse(u, h.getOrElse(s, f.getOrElse(u, "")))
    
    val s = f(Symbol).toUpperCase
    USPCHit(score,
      HitSymbol(s, s),
      getH(ClassTitle, ClassTitleUnstemmed), 
      getH(SubClassTitle, SubClassTitleUnstemmed), 
      getH(SubClassDescription, SubClassDescriptionUnstemmed),
      getH(Text, TextUnstemmed)
    )
  }

  /** Entity class mapping to a database row representing a USPC Symbol.
    */
  case class UsClass(id: Option[Int], xmlId: String, parentXmlId: String, symbol: String, classTitle: Option[String], subClassTitle: Option[String], subClassDescription: Option[String], text: String) {
    def toDescription(f: String => String) = USPCDescription(id.get, symbol, classTitle.getOrElse(""), subClassTitle.map(f).getOrElse(""), subClassDescription.map(f).getOrElse(""), f(text))
  } 
}
