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

import org.scalatest.{ FlatSpec, Matchers }
import org.slf4j.LoggerFactory
import org.t3as.patClas.common.USPCTypes.UsClass
import scala.collection.immutable.IndexedSeq
import scala.xml.XML
import java.io.StringReader

class TestUSPCParser extends FlatSpec with Matchers {
  val log = LoggerFactory.getLogger(getClass)

    "UsClass as Scala XML" should "parse OK" in {
      nextId = 0
      USPCParser.parse(elem, check)
    }
  
    // We need tag soup because zip entry: classdefs201308/class_106.xml (and others) have unclosed graphics tags:
    //   org.xml.sax.SAXParseException; lineNumber: 1645; columnNumber: 3; The element type "graphic" must be terminated by the matching end-tag "</graphic>"
    "UsClass as string" should "parse with tagsoup" in {
      nextId = 0
      USPCParser.parse(
          XML.withSAXParser(new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl().newSAXParser()).
          	load(new StringReader(elem.toString)), // this way tagsoup appears not to wrap html/body around our top level class tag
      	check)
    }

  "class_100.xml file" should "parse with tagsoup" in {
    val f = new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl()
    def l(name: String) = log.debug(s"feature $name = ${f.getFeature("http://www.ccil.org/~cowan/tagsoup/features/" + name)}")
    l("ignore-bogons")
    l("root-bogons")
    // These default feature settings from org.ccil.cowan.tagsoup.Parser seem OK for our purpose:
    //	private static boolean DEFAULT_NAMESPACES = true;
    //	private static boolean DEFAULT_IGNORE_BOGONS = false;
    //	private static boolean DEFAULT_BOGONS_EMPTY = false;
    //        private static boolean DEFAULT_ROOT_BOGONS = true; // shouldn't this stop our top level class from being wrapped in html/body?
    //	private static boolean DEFAULT_DEFAULT_ATTRIBUTES = true;
    //	private static boolean DEFAULT_TRANSLATE_COLONS = false;
    //	private static boolean DEFAULT_RESTART_ELEMENTS = true;
    //	private static boolean DEFAULT_IGNORABLE_WHITESPACE = false;
    //	private static boolean DEFAULT_CDATA_ELEMENTS = true;
    val in = getClass.getResourceAsStream("/class_106.xml")
    val root = XML.withSAXParser(f.newSAXParser()).load(in)
    log.debug("root.label = " + root.label)
    val usClass = if (root.label == "html") (root \ "body" \ "class")(0) else root
    // log.debug("usClass = " + usClass.toString)
    USPCParser.parse(usClass, (c: UsClass) => log.debug(s"symbol = ${c.symbol}"))
    in.close
  }
  
  "checkId" should "generate xmlIds" in {
    Seq(
        // test classnum variation
        ("C001S104130", "1", "104.13", "C001S104130"),
        ("C016S104130", "16", "104.13", "C016S104130"),
        ("C165S104130", "165", "104.13", "C165S104130"),
        ("CD01S104130", "D1", "104.13", "CD01S104130"),
        ("CD16S104130", "D16", "104.13", "CD16S104130"),
        ("CPLTS10413x", "PLT", "104.13", "CPLTS104130"), // warning because provided xmlId doesn't match
        ("CPLTS10413x", "H1", "104.13", "CPLTS10413x"),  // invalid classnum leaves xmlId unchanged (even though its invalid)

        // test subclass dot num variation
        ("C165S104000", "165", "104", "C165S104000"),
        ("C165S104100", "165", "104.1", "C165S104100"),
        ("C165S104130", "165", "104.13", "C165S104130"),
        ("C165S104000", "165", "104.", "C165S104000"),    // trailing '.'
        ("C165S000130", "165", ".13", "C165S000130"),     // leading '.'
        ("C165S10413x", "165", "104.133", "C165S104133"), // warning because provided xmlId doesn't match
        ("C165S10413x", "165", "1H.133", "C165S10413x"),  // invalid classnum leaves xmlId unchanged (even though its invalid)
        ("C165S10413x", "165", "104.1H", "C165S10413x")   // invalid classnum leaves xmlId unchanged (even though its invalid)
    ).foreach { p =>
      USPCParser.checkId(p._1, p._2, p._3) should be (p._4)
    }
  }
  
  val elem = <class classnum="2" id="C002S000000">
               <title>APPAREL</title>
               <sections>
                 <descsect>
                   <para>
                     This is the generic class for garments and other devices to
				be worn by mankind to adorn, cover or protect the body or person.
				Included within the class are (1) such garments or devices, per
				se, (2) combinations of such garments or devices with other things
				where the combination is not elsewhere provided for, (3) processes
				of, and patterns for making such garments or devices, (4)
				subcombinations
				of garments and the like, not elsewhere provided for and processes
				of manufacture relating to such subcombinations, and (5) garment
				supporters
				and retainers (see search notes below).
                   </para>
                 </descsect>
                 <notesect>
                   <notes>
                     <note1 notenum="(1)">
                       <para>
                         Note. In general, processes of making particular types of
						garments have been classified in the subclass pertaining to the
						particular
						garment made.
                       </para>
                     </note1>
                   </notes>
                 </notesect>
                 <stcssect>
                   <srchin>
                     <srecord>
                       <subnum range="p" ref="C002S243100" doc="Class002.sgm">243.1</subnum>
                       <descript>
                         <para>
                           for
							processes applicable to a number of different
							types of garments. Subcombinations of particular garments have been
							classified in the garment subclass unless such subcombinations
							have
							been specifically provided for in the subclasses indented under
							subclass 243.1. Subcombinations applicable to a number of
							different
							types of garments have been classified in subclasses 243.1&plus;.
                         </para>
                       </descript>
                     </srecord>
                     <srecord>
                       <subnum ref="C002S300000" doc="Class002.sgm">300</subnum>
                       <descript>
                         <para>through 342, for garment supporters and retainers.</para>
                       </descript>
                     </srecord>
                   </srchin>
                 </stcssect>
                 <scsect>
                   <srchout>
                     <crecord>
                       <classnum ref="C024S000000" doc="Class024.sgm">24</classnum>
                       <subref>
                         <clstitle>Buckles, Buttons, Clasps, etc.</clstitle>
                       </subref>
                       <descript>
                         <para>
                           for a single or combined fastener or retainer device
							used to support or retain a garment or to hold parts of a garment
							supporter together where no significant structure or feature of
							the garment or part held is claimed. These include (a) two
							fastening
							devices even though connected by a single strand; (b) a device to
							hold the ends of a belt, strip, or strand; (c) a device to hold
							the ends of a belt, strip, or strand together to form a loop even
							though claimed in combination with such belt, strip, or strand;
							or (d) a single or combined fastening means even though
							supporting or
							fastening together two or more garments or parts of a garment supporter.
							Class 2 takes all other garment supporters or retainers or parts
							thereof
							not provided for.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C036S000000" doc="Class036.sgm">36</classnum>
                       <subref>
                         <clstitle>Boots, Shoes, and Leggings</clstitle>
                         <pretext></pretext>
                       </subref>
                       <descript>
                         <para>
                           for miscellaneous footwear, such as boots, shoes,
							and leggings.
                         </para>
                       </descript>
                     </crecord>
                   </srchout>
                 </scsect>
               </sections>
               <sclasses>
                 <subclass subnum="1" id="C002S001000">
                   <sctitle>MISCELLANEOUS:</sctitle>
                   <parent ref="C002S000000" doc="Class002.sgm">the class definition</parent>
                   <scdesc>
                     Miscellaneous apparel and analogous devices not elsewhere
			provided for.
                   </scdesc>
                 </subclass>
                 <subclass subnum="2.11" id="C002S002110">
                   <sctitle>Astronaut&quot;s body cover:</sctitle>
                   <parent ref="C002S455000" doc="Class002.sgm">subclass 455</parent>
                   <scdesc>
                     Guard or protector comprising a body suit which covers the
			trunk, arms, and legs of the wearer and which is intended and
			constructed to
			be worn above the Earth&quot;s atmosphere.
                   </scdesc>
                   <srchout>
                     <crecord>
                       <classnum ref="C128S000000" doc="Class128.sgm">128</classnum>
                       <subref>
                         <clstitle>Surgery</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C128S200240" doc="Class128.sgm">200.24</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a device for supplying a breathable gas to
						a person; especially, subclass 201.19 for a device to transmit or
						facilitate
						voice transmission from a face mask, hood, or helmet, and subclasses
						201.29&plus;
						for a body supported head covering and a body garment;
						in particular, subclass 202.11 where the body garment is a flight
						suit.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C219S000000" doc="Class219.sgm">219</classnum>
                       <subref>
                         <clstitle>Electric Heating</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C219S211000" doc="Class219.sgm">211</subnum>
                       </subref>
                       <descript>
                         <para> for an electrically heated garment.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C250S000000" doc="Class250.sgm">250</classnum>
                       <subref>
                         <clstitle>Radiant Energy</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C250S516100" doc="Class250.sgm">516.1</subnum>
                       </subref>
                       <descript>
                         <para> for a radiation shielded garment.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C414S000000" doc="Class414.sgm">414</classnum>
                       <subref>
                         <clstitle>Material or Article Handling</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C414S001000" doc="Class414.sgm">1</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a hand controlled article manipulator and subclass
						8 for a hand controlled article manipulator combined with a
						radiation
						shield.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C600S000000" doc="Class600.sgm">600</classnum>
                       <subref>
                         <clstitle>Surgery</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C600S019000" doc="Class600.sgm">19</subnum>
                       </subref>
                       <descript>
                         <para> for subject matter relating to an anti-G suit.</para>
                       </descript>
                       xml
                     </crecord>
                   </srchout>
                 </subclass>
                 <subclass subnum="2.12" id="C002S002120">
                   <sctitle>Having relatively rotatable coaxial coupling component:</sctitle>
                   <parent ref="C002S002110" doc="Class002.sgm">subclass 2.11</parent>
                   <scdesc>
                     Astronaut&quot;s body cover wherein the body suit includes
			a trunk covering part and an appendage covering part (e.g., arm,
			leg, head) which parts are tubular at their point of connection and
			relatively rotatable about their common axis.
                   </scdesc>
                 </subclass>
                 <subclass subnum="2.13" id="C002S002130">
                   <sctitle>Having convoluted component:</sctitle>
                   <parent ref="C002S002110" doc="Class002.sgm">subclass 2.11</parent>
                   <scdesc>
                     Astronaut&quot;s body cover wherein one part of the body
			suit is made movable relative to another part of the body suit by
			a series of adjacent ridges and valleys formed in it and wherein
			the ridges are movable toward or away from adjacent ridges.
                   </scdesc>
                   <srchout>
                     <crecord>
                       <classnum ref="C138S000000" doc="Class138.sgm">138</classnum>
                       <subref>
                         <clstitle>Pipes and Tubular Conduits</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C138S121000" doc="Class138.sgm">121</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a corrugated flexible tube or pipe of general
						utility.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C285S000000" doc="Class285.sgm">285</classnum>
                       <subref>
                         <clstitle>Pipe Joints or Couplings</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C285S226000" doc="Class285.sgm">226</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a bellow type flexible joint between rigid
						members.
                         </para>
                       </descript>
                     </crecord>
                   </srchout>
                 </subclass>
                 <subclass subnum="2.14" id="C002S002140">
                   <sctitle>Aviator&quot;s body cover:</sctitle>
                   <parent ref="C002S455000" doc="Class002.sgm">subclass 455</parent>
                   <scdesc>
                     Guard or protector comprising a body suit which covers the
			trunk, arms, and legs of the wearer and which is intended and
			constructed to
			be worn by an aircraft crew member within the bounds of the
			Earth&quot;s
			atmosphere.
                   </scdesc>
                   <srchin>
                     <srecord>
                       <subnum range="p" ref="C002S006100" doc="Class002.sgm">6.1</subnum>
                       <descript>
                         <para>for an aviator&quot;s helmet.</para>
                       </descript>
                     </srecord>
                   </srchin>
                   <srchout>
                     <crecord>
                       <classnum ref="C128S000000" doc="Class128.sgm">128</classnum>
                       <subref>
                         <clstitle>Surgery</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C128S200240" doc="Class128.sgm">200.24</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a device for supplying a breathable gas to
						a person; especially, subclass 201.19 for a device for transmitting
						voice
						communication from a face mask, helmet, or hood, and subclasses 201.29&plus;
						for
						a body covering and a body supported head covering; in particular,
						subclass 202.11 for a flight suit.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C219S000000" doc="Class219.sgm">219</classnum>
                       <subref>
                         <clstitle>Electric Heating</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C219S211000" doc="Class219.sgm">211</subnum>
                       </subref>
                       <descript>
                         <para> for an electrically heated garment.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C244S000000" doc="Class244.sgm">244</classnum>
                       <subref>
                         <clstitle>Aeronautics and Astronautics</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C244S143000" doc="Class244.sgm">143</subnum>
                       </subref>
                       <descript>
                         <para> for a garment attached to a parachute.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C600S000000" doc="Class600.sgm">600</classnum>
                       <subref>
                         <clstitle>Surgery</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C600S019000" doc="Class600.sgm">19</subnum>
                       </subref>
                       <descript>
                         <para> for subject matter relating to an anti-G suit.</para>
                       </descript>
                     </crecord>
                   </srchout>
                 </subclass>
                 <subclass subnum="2.15" id="C002S002150">
                   <sctitle>Underwater diver&quot;s body cover:</sctitle>
                   <parent ref="C002S455000" doc="Class002.sgm">subclass 455</parent>
                   <scdesc>
                     Guard or protector comprising a body enclosing suit which
			covers the trunk, arms, and legs of the wearer and which is intended
			and constructed to be worn by a diver underwater.
                   </scdesc>
                   <srchout>
                     <crecord>
                       <classnum ref="C128S000000" doc="Class128.sgm">128</classnum>
                       <subref>
                         <clstitle>Surgery</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C128S200240" doc="Class128.sgm">200.24</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a device for supplying a breathable gas to
						a person; especially, subclass 200.29 for a device for dispersing
						exhaust
						gases from a diver&quot;s mask, helmet, or other underwater
						apparatus into ambient water, subclass 201.11 for a draw type snorkel,
						and subclass 201.27 for a diver&quot;s body covering having
						a body supported head covering.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C219S000000" doc="Class219.sgm">219</classnum>
                       <subref>
                         <clstitle>Electric Heating</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C219S211000" doc="Class219.sgm">211</subnum>
                       </subref>
                       <descript>
                         <para> for electrically heated wearing apparel.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C340S000000" doc="Class340.sgm">340</classnum>
                       <subref>
                         <clstitle>Communications: Electrical</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C340S850000" doc="Class340.sgm">850</subnum>
                       </subref>
                       <descript>
                         <para> for an underwater communication device.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C351S000000" doc="Class351.sgm">351</classnum>
                       <subref>
                         <clstitle>
                           Optics: Eye Examining, Vision Testing and Correcting
                         </clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C351S043000" doc="Class351.sgm">43</subnum>
                       </subref>
                       <descript>
                         <para> for glasses designed for underwater use.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C367S000000" doc="Class367.sgm">367</classnum>
                       <subref>
                         <clstitle>
                           Communications, Electrical: Acoustic Wave Systems
						and Devices
                         </clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C367S131000" doc="Class367.sgm">131</subnum>
                       </subref>
                       <descript>
                         <para> for an acoustic underwater communication device.</para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C379S000000" doc="Class379.sgm">379</classnum>
                       <subref>
                         <clstitle>Telephonic Communications</clstitle>
                         <pretext></pretext>
                         <subnum range="s" ref="C379S175000" doc="Class379.sgm">175</subnum>
                       </subref>
                       xml
                       <descript>
                         <para>
                           for telephonic communication equipment for underwater
						use (i.e., in a diving suit, etc.) and subclass 430 for a body
						supported
						communication device (i.e., in headwear, etc.).
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C405S000000" doc="Class405.sgm">405</classnum>
                       <subref>
                         <clstitle>Hydraulic and Earth Engineering</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C405S185000" doc="Class405.sgm">185</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a diving suit combined with a grapple, underwater power
						propulsion device, elevator, or other means pertaining to
						hydraulic engineering.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C414S000000" doc="Class414.sgm">414</classnum>
                       <subref>
                         <clstitle>Material or Article Handling</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C414S001000" doc="Class414.sgm">1</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a hand controlled article manipulator and subclass
						8 for a hand controlled article manipulator combined with a
						radiation
						shield.
                         </para>
                       </descript>
                     </crecord>
                     <crecord>
                       <classnum ref="C441S000000" doc="Class441.sgm">441</classnum>
                       <subref>
                         <clstitle>Buoys, Rafts, and Aquatic Devices</clstitle>
                         <pretext></pretext>
                         <subnum range="p" ref="C441S088000" doc="Class441.sgm">88</subnum>
                       </subref>
                       <descript>
                         <para>
                           for a personal flotation device and subclasses
						102&plus;
						for a body garment having buoyancy means to sustain
						the user floating in the water.
                         </para>
                       </descript>
                     </crecord>
                   </srchout>
                 </subclass>
               </sclasses>
             </class>

  val expected = IndexedSeq(
    ("C002S000000", "none", "2", Some("APPAREL"), None),
    ("C002S001000", "C002S000000", "2/1", None, Some("<sctitle>MISCELLANEOUS:</sctitle>")),
    ("C002S002110", "C002S455000", "2/2.11", None, Some("<sctitle>Astronaut&quot;s body cover:</sctitle>")),
    ("C002S002120", "C002S002110", "2/2.12", None, Some("<sctitle>Having relatively rotatable coaxial coupling component:</sctitle>")),
    ("C002S002130", "C002S002110", "2/2.13", None, Some("<sctitle>Having convoluted component:</sctitle>")),
    ("C002S002140", "C002S455000", "2/2.14", None, Some("<sctitle>Aviator&quot;s body cover:</sctitle>")),
    ("C002S002150", "C002S455000", "2/2.15", None, Some("<sctitle>Underwater diver&quot;s body cover:</sctitle>")))

  def check(c: UsClass) = {
    // log.debug(s"id = ${nextId}, xmlId = ${c.xmlId}, parentId = ${c.parentXmlId}, symbol = ${c.symbol}, classTitle = ${c.classTitle.getOrElse("")}, subClassTitle = ${c.subClassTitle.getOrElse("")}, text = ${c.text}")
    val exp = expected(nextId)
    c.xmlId should be(exp._1)
    c.parentXmlId should be(exp._2)
    c.classTitle should be(exp._4)
    c.subClassTitle should be(exp._5)
    nextId += 1
  }
  var nextId = 0

}
