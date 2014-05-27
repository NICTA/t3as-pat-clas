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

package org.t3as.patClas.service

import org.slf4j.LoggerFactory

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import javax.ws.rs.{Consumes, Produces}
import javax.ws.rs.core.MediaType
import javax.ws.rs.ext.{ContextResolver, Provider}


// Provide a JSON mapper for use by JAX-WS
@Provider
@Consumes(Array(MediaType.APPLICATION_JSON))
@Produces(Array(MediaType.APPLICATION_JSON))
class JSONContextResolver extends ContextResolver[ObjectMapper] {

  val log = LoggerFactory.getLogger(getClass)

  val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m
  }

  def getContext(clazz: Class[_]) = {
    log.debug(s"getContext: clazz = $clazz")
    mapper
  }
}


// Provide an XML mapper for use by JAX-WS
// TODO: So far this just outputs "<item />" for each case class object, no fields.
//@Provider
//@Produces(Array(MediaType.APPLICATION_XML))
//class XMLContextResolver extends ContextResolver[ObjectMapper] {
//
//  val log = LoggerFactory.getLogger(getClass)
//
//  val mapper = {
//    val m = new XmlMapper() with ScalaObjectMapper
//
//    m.registerModule(DefaultScalaModule)
//    m
//  }
//
//  def getContext(clazz: Class[_]) = {
//    log.debug("getting Jackson XmlMapper")
//    mapper
//  }
//}
