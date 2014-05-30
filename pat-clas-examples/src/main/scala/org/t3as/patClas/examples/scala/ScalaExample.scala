package org.t3as.patClas.examples.scala

import org.t3as.patClas.service.PatClasService
import org.slf4j.LoggerFactory
import org.t3as.patClas.api.{CPC, IPC}
import org.t3as.patClas.api.API.{HitBase, SearchService, LookupService, Factory}
import org.t3as.patClas.client.PatClasClient

object ScalaExample {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) = {
    // local (in-process) service
    val local = PatClasService.factory
    try doit(local)
    finally local.close
    
    // client that makes HTTP requests to remote (inter-process) service (which has to be running elsewhere)
    val remote = new PatClasClient("http://localhost:8080/pat-clas-service/rest/v1.0")
    try doit(remote)
    finally remote.close
  }

  def doit(f: Factory) = {
      val hits = f.cpc.search("locomotive")
      log.info(s"top score: ${hits(0).score}")
      log.info(s"hits: $hits")

      val descr = f.ipc.children(0, "XML")
      log.info(s"descr: $descr")
  }

}