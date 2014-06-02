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

package org.t3as.patClas.examples.javaApi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.t3as.patClas.api.CPC;
import org.t3as.patClas.api.IPC;
import org.t3as.patClas.api.javaApi.Factory;
import org.t3as.patClas.service.PatClasService$; // PatClasService$.MODULE$ is how to access a Scala "object" singleton
import org.t3as.patClas.client.PatClasClient$;

public class JavaExample {
    static final Logger log = LoggerFactory.getLogger(JavaExample.class);

    public static void main(String[] args) {
        // local (in-process) service
        PatClasService$.MODULE$.init();
        Factory local = PatClasService$.MODULE$.service().toJavaApi();
        try {
            doit(local);
        } finally {
            local.close();
        }

        // client that makes HTTP requests to remote (inter-process) service
        // (which has to be running elsewhere)
        Factory remote = PatClasClient$.MODULE$.toJavaApi("http://localhost:8080/pat-clas-service/rest/v1.0");
        try {
            doit(remote);
        } finally {
            remote.close();
        }
    }

    private static void doit(Factory f) {
        List<CPC.Hit> hits = f.getCPCSearch().search("locomotive");
        log.info("top score: " + hits.get(0).score());
        log.info("hits: " + hits.toString());

        List<IPC.Description> descr = f.getIPCLookup().children(0, "XML");
        log.info("descr: " + descr.toString());
    }
}
