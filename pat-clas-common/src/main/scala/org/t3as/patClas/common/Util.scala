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
import scala.xml.{Utility, XML}

import org.slf4j.LoggerFactory

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
