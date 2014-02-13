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

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

class TestIPCParser extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "ipc xml" should "parse OK" in {
    val elem = <staticIpc edition="20130101" lang="EN" documentRoot="">
                 <ipcEntry kind="s" symbol="A" ipcLevel="A" entryType="K" lang="EN">
                   <textBody>
                     <title>
                       <titlePart>
                         <text>SECTION A <emdash/> HUMAN NECESSITIES</text>
                       </titlePart>
                     </title>
                   </textBody>
                   <ipcEntry kind="t" symbol="A01" ipcLevel="A" entryType="K" lang="EN">
                     <textBody>
                       <title>
                         <titlePart>
                           <text>AGRICULTURE</text>
                         </titlePart>
                       </title>
                     </textBody>
                   </ipcEntry>
                   <ipcEntry kind="c" symbol="A01" ipcLevel="A" entryType="K" lang="EN">
                     <textBody>
                       <title>
                         <titlePart>
                           <text>AGRICULTURE</text>
                         </titlePart>
                         <titlePart>
                           <text>FORESTRY</text>
                         </titlePart>
                         <titlePart>
                           <text>ANIMAL HUSBANDRY</text>
                         </titlePart>
                         <titlePart>
                           <text>HUNTING</text>
                         </titlePart>
                         <titlePart>
                           <text>TRAPPING</text>
                         </titlePart>
                         <titlePart>
                           <text>FISHING</text>
                         </titlePart>
                       </title>
                     </textBody>
                     <ipcEntry kind="u" symbol="A01B" ipcLevel="A" entryType="K" lang="EN">
                       <textBody>
                         <title>
                           <titlePart>
                             <text>SOIL WORKING IN AGRICULTURE OR FORESTRY</text>
                           </titlePart>
                           <titlePart>
                             <text>PARTS, DETAILS, OR ACCESSORIES OF AGRICULTURAL MACHINES OR IMPLEMENTS, IN GENERAL</text>
                             <entryReference>
                               making or covering furrows or holes for sowing, planting or manuring<sref ref="A01C0005000000"/>
                             </entryReference>
                             <entryReference>
                               machines for harvesting root crops<sref ref="A01D"/>
                             </entryReference>
                             <entryReference>
                               mowers convertible to soil working apparatus or capable of soil working<sref ref="A01D0042040000"/>
                             </entryReference>
                             <entryReference>
                               mowers combined with soil working implements<sref ref="A01D0043120000"/>
                             </entryReference>
                             <entryReference>
                               soil working for engineering purposes<sref ref="E01"/>
                               ,<sref ref="E02"/>
                               ,<sref ref="E21"/>
                             </entryReference>
                           </titlePart>
                         </title>
                       </textBody>
                       <ipcEntry kind="i" symbol="A01B" ipcLevel="A" entryType="K" lang="EN">
                         <textBody>
                           <index>
                             <indexEntry>
                               <text>HAND TOOLS </text>
                               <references>
                                 <sref ref="A01B0001000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>PLOUGHS</text>
                               <indexEntry>
                                 <text>General construction </text>
                                 <references>
                                   <sref ref="A01B0003000000"/>
                                   ,<sref ref="A01B0005000000"/>
                                   ,<sref ref="A01B0009000000"/>
                                   ,<sref ref="A01B0011000000"/>
                                 </references>
                               </indexEntry>
                               <indexEntry>
                                 <text>Special adaptations </text>
                                 <references>
                                   <sref ref="A01B0013000000"/>
                                   ,<sref ref="A01B0017000000"/>
                                 </references>
                               </indexEntry>
                               <indexEntry>
                                 <text>Details </text>
                                 <references>
                                   <sref ref="A01B0015000000"/>
                                 </references>
                               </indexEntry>
                             </indexEntry>
                             <indexEntry>
                               <text>HARROWS</text>
                               <indexEntry>
                                 <text>General construction </text>
                                 <references>
                                   <sref ref="A01B0019000000"/>
                                   ,<sref ref="A01B0021000000"/>
                                 </references>
                               </indexEntry>
                               <indexEntry>
                                 <text>Special applications </text>
                                 <references>
                                   <sref ref="A01B0025000000"/>
                                 </references>
                               </indexEntry>
                               <indexEntry>
                                 <text>Details </text>
                                 <references>
                                   <sref ref="A01B0023000000"/>
                                 </references>
                               </indexEntry>
                             </indexEntry>
                             <indexEntry>
                               <text>IMPLEMENTS USABLE EITHER AS PLOUGHS OR AS HARROWS OR THE LIKE </text>
                               <references>
                                 <sref ref="A01B0007000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>OTHER MACHINES </text>
                               <references>
                                 <mref ref="A01B0027000000" endRef="A01B0045000000"/>
                                 ,<sref ref="A01B0049000000"/>
                                 ,<sref ref="A01B0077000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>ELEMENTS OR PARTS OF MACHINES OR IMPLEMENTS </text>
                               <references>
                                 <mref ref="A01B0059000000" endRef="A01B0071000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>TRANSPORT IN AGRICULTURE </text>
                               <references>
                                 <sref ref="A01B0051000000"/>
                                 ,<sref ref="A01B0073000000"/>
                                 ,<sref ref="A01B0075000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>OTHER PARTS, DETAILS OR ACCESSORIES OF AGRICULTURAL MACHINES OR IMPLEMENTS </text>
                               <references>
                                 <sref ref="A01B0076000000"/>
                               </references>
                             </indexEntry>
                             <indexEntry>
                               <text>PARTICULAR METHODS FOR WORKING SOIL </text>
                               <references>
                                 <sref ref="A01B0047000000"/>
                                 ,<sref ref="A01B0079000000"/>
                               </references>
                             </indexEntry>
                           </index>
                         </textBody>
                       </ipcEntry>
                       <ipcEntry kind="m" symbol="A01B0001000000" ipcLevel="A" entryType="K" lang="EN" priorityOrder="250">
                         <textBody>
                           <title>
                             <titlePart>
                               <text>Hand tools</text>
                               <entryReference>
                                 edge trimmers for lawns<sref ref="A01G0003060000"/>
                               </entryReference>
                             </titlePart>
                           </title>
                         </textBody>
                       </ipcEntry>
                     </ipcEntry>
                   </ipcEntry>
                 </ipcEntry>
               </staticIpc>

    val trees = IPCParser.parse(elem)
    // log.debug(s"trees = $trees")
    trees.size should be(1)
    trees(0).children.size should be(2)

    val n0 = trees(0).children(0)
    n0.value.symbol should be("A01")
    n0.value.kind should be("t")
    n0.children.size should be(0)
    
    val n1 = trees(0).children(1)
    n1.value.symbol should be("A01")
    n1.value.kind should be("c")
    n1.children.size should be(1)
    
    val n2 = n1.children(0)
    n2.value.symbol should be("A01B")
    n2.value.kind should be("u")
    n2.children.size should be(2)
    
    val n3 = n2.children(0)
    n3.value.symbol should be("A01B")
    n3.value.kind should be("i")
    n3.children.size should be(0)
    
    val n4 = n2.children(1)
    n4.value.symbol should be("A01B0001000000")
    n4.value.kind should be("m")
    n4.children.size should be(0)    
  }

}
