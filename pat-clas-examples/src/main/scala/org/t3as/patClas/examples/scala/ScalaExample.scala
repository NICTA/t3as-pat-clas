package org.t3as.patClas.examples.scala

import org.t3as.patClas.service.CPCService
import org.t3as.patClas.service.IPCService
import org.t3as.patClas.client.CPCClient
import org.t3as.patClas.client.IPCClient
import org.t3as.patClas.service.PatClasService
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.{CPC, IPC}
import org.t3as.patClas.common.API.{HitBase, SearchService, LookupService}

object ScalaExample {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) = {
    // local (in-process) service
//    try doit(new CPCService, new IPCService)
//    finally PatClasService.close
    
    // client that makes HTTP requests to remote (inter-process) service (which has to be running elsewhere)
    doit(new CPCClient, new IPCClient)
  }

  def doit(cpc: SearchService[CPC.Hit] with LookupService[CPC.Description], ipc: SearchService[IPC.Hit] with LookupService[IPC.Description]) = {
      val hits = cpc.search("locomotive")
      log.info(s"top score: ${hits(0).score}")
      log.info(s"hits: $hits")

      val descr = ipc.children(0, "XML")
      log.info("descr: $descr")
  }

}