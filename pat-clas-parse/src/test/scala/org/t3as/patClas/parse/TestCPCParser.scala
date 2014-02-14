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

import org.t3as.patClas.common.CPCTypes.ClassificationItem
import org.t3as.patClas.common.TreeNode

class TestCPCParser extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

  "cpc xml" should "parse OK" in {
    val elem = <class-scheme><classification-item breakdown-code="false" not-allocatable="true" level="5" additional-only="false" sort-key="B29C" date-revised="2013-01-01">
    		<classification-symbol>B29C</classification-symbol>
    		<class-title date-revised="2013-01-01">
    			<title-part>
    				<text scheme="ipc">SHAPING OR JOINING OF PLASTICS</text>
    			</title-part>
    			<title-part><text scheme="ipc">SHAPING OF SUBSTANCES IN A PLASTIC STATE, IN GENERAL</text></title-part>
    			<title-part>
    				<text scheme="ipc">AFTER-TREATMENT OF THE SHAPED PRODUCTS, e.g. REPAIRING</text>
    				<reference>
    					<CPC-specific-text>
    						<text scheme="cpc"> moulding devices for producing toilet or cosmetic sticks
    							<class-ref scheme="cpc">A45D40/16</class-ref>
    						</text>
    					</CPC-specific-text>
    					<text scheme="ipc"> ; working in the manner of metal
    						<class-ref scheme="cpc">B23</class-ref>; grinding, polishing 
    						<class-ref scheme="cpc">B24</class-ref>; cutting 
    						<class-ref scheme="cpc">B26D</class-ref>, 
    						<class-ref scheme="cpc">B26F</class-ref>; making preforms 
    						<class-ref scheme="cpc">B29B11/00</class-ref> ; making laminated products by combining previously unconnected layers which become one product whose layers will remain together 
    						<class-ref scheme="cpc">B32B37/00</class-ref> - 
    						<class-ref scheme="cpc">B32B41/00</class-ref>
    					</text>
    				</reference>
    			</title-part>
    		</class-title>
    		<notes-and-warnings date-revised="2013-01-01"><note type="note"><note-paragraph><pre><br/>
						1. Attention is drawn to Note (3) following the title of class 
    					<class-ref scheme="cpc">B29</class-ref>.
						<br/><br/>
						2. In this subclass:
						<br/>
						- repairing of articles made from plastics or substances in
						<br/>
						a plastic state, e.g. of articles shaped or produced by
						<br/>
						using techniques covered by this subclass or subclass <class-ref scheme="cpc">B29D</class-ref>,
						<br/>
						is classified in group
						<class-ref scheme="cpc">B29C73/00</class-ref>
						;
						<br/>
						- component parts, details, accessories or auxiliary
						<br/>
						operations which are applicable to more than one moulding
						<br/>
						technique a reclassified in groups
						<class-ref scheme="cpc">B29C31/00</class-ref>
						to
						<class-ref scheme="cpc">B29C37/00</class-ref>
						;
						<br/>
						- component parts, details, accessories or auxiliary
						<br/>
						operations which are only of use for one specific shaping
						<br/>
						technique a reclassified only in the relevant subgroups of
						<br/>
						groups
						<class-ref scheme="cpc">B29C39/00</class-ref>
						to
						<class-ref scheme="cpc">B29C71/00</class-ref>
						.
						<br/></pre><br/></note-paragraph></note>
    		</notes-and-warnings>
    		<classification-item breakdown-code="false" not-allocatable="false" level="6" additional-only="false" sort-key="B29C31/00" date-revised="2013-01-01">
    			<classification-symbol>B29C31/00</classification-symbol>
    			<class-title date-revised="2013-01-01"><title-part><text scheme="ipc">Component parts, details or accessories</text></title-part><title-part><text scheme="ipc">Auxiliary operations</text></title-part></class-title>
    			<notes-and-warnings date-revised="2013-01-01"><note type="note"><note-paragraph>Attention is drawn to Note (3) following the subclass title</note-paragraph></note></notes-and-warnings>
    			<classification-item breakdown-code="false" not-allocatable="false" level="7" additional-only="false" sort-key="B29C31/00" date-revised="2013-01-01">
    				<classification-symbol>B29C31/00</classification-symbol>
    				<class-title date-revised="2013-01-01"><title-part><text scheme="ipc">Handling, e.g. feeding of the material to be shaped,</text><CPC-specific-text><text scheme="cpc">storage of plastics material before moulding; Automation, i.e. automated handling lines in plastics processing plants, e.g. using manipulators or robots</text><reference><text scheme="cpc"> discharging moulded articles from the mould <class-ref scheme="cpc">B29C37/0003</class-ref> ; storage of prepregs or SMC after impregnation or during ageing <class-ref scheme="cpc">B29C70/54</class-ref> ; baling of rubber <class-ref scheme="cpc">B29B15/02</class-ref> ; in general <class-ref scheme="cpc">B65G</class-ref></text></reference></CPC-specific-text></title-part></class-title>
    				<classification-item breakdown-code="false" not-allocatable="false" level="8" additional-only="false" sort-key="B29C31/002" date-revised="2013-01-01">
    					<classification-symbol>B29C31/002</classification-symbol>
    					<class-title date-revised="2013-01-01"><title-part><CPC-specific-text><text scheme="cpc">Handling tubes, e.g. transferring between shaping stations, loading on mandrels</text></CPC-specific-text></title-part></class-title>
              		</classification-item>
    			</classification-item>
    		</classification-item>
    	</classification-item></class-scheme>
      
      val trees = CPCParser.parse(elem)
      // log.debug(s"trees = $trees")
      trees.size should be(1)
    
      val treeAsSeq = flatten(trees(0))
      // log.debug(s"actual = $actual")
      treeAsSeq.size should be(4)
      val expected = Seq(
        ClassificationItem(None, 0, false, false, false, "2013-01-01", 5, "B29C", "", ""),
        ClassificationItem(None, 0, false, true, false, "2013-01-01", 6, "B29C31/00", "", ""),
        ClassificationItem(None, 0, false, true, false, "2013-01-01", 7, "B29C31/00", "", ""), 
        ClassificationItem(None, 0, false, true, false, "2013-01-01", 8, "B29C31/002", "", ""))
      treeAsSeq zip expected foreach { case (a, e) =>
        a.breakdownCode should be (e.breakdownCode)
        a.allocatable should be (e.allocatable)
        a.additionalOnly should be (e.additionalOnly)
        a.level should be (e.level)
        a.symbol should be (e.symbol)
    }
    
  }

  def flatten(n: TreeNode[ClassificationItem]): Seq[ClassificationItem] = {
    if (n.value.level == 8) n.children.size should be (0)
    else n.children.size should be (1)
    
    val x = n.children.flatMap(flatten(_))
    x.+:(n.value)
  }

}
