package org.t3as.patClas.common.search

import java.io.File

import org.apache.lucene.document.{Document, Field}
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.t3as.patClas.common.{TreeNode, Util}
import org.t3as.patClas.common.CPCTypes.ClassificationItem
import org.t3as.patClas.common.CPCTypes.IndexFieldName.{ClassTitle, Level, NotesAndWarnings, Symbol, convert}

import Indexer.highlightFieldType
import resource.managed

object RAMIndex {

  val xml = """<class-title date-revised="2013-01-01">
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
            </class-title>"""

  def toDoc(c: ClassificationItem) = {
    val doc = new Document
    doc add new StringField(Symbol, c.symbol, Store.YES)
    doc add new StringField(Level, c.level.toString, Store.YES)

    if (!c.classTitle.isEmpty()) doc.add(new Field(ClassTitle, Util.toText(c.classTitle), highlightFieldType))
    if (!c.notesAndWarnings.isEmpty()) doc.add(new Field(NotesAndWarnings, Util.toText(c.notesAndWarnings), highlightFieldType))
    doc
  }

  def makeTestIndex: Directory = {
    val dir = new RAMDirectory
    
    for (indexer <- managed(new Indexer[ClassificationItem](new File("not.used"), toDoc) {
      override def open = new IndexWriter(dir, indexWriterConfig)
    })) {
      val title8 = xml
        
      val notes = """<notes-and-warnings date-revised="2013-01-01"><note type="note"><note-paragraph><pre><br/>
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
            </notes-and-warnings>"""
        
      val level8 = TreeNode(ClassificationItem(None, -1, false, true,  false, "2013-01-01", 8, "B29C31/002", title8, notes), Seq())
      
      val title7 = """<class-title date-revised="2013-01-01">
                <title-part>
                    <text scheme="ipc">faking text for title7</text>
                </title-part>
            </class-title>"""
      
      val notes2 = """<notes-and-warnings date-revised="2013-01-01">look out!</notes-and-warnings>"""
      val level7 = TreeNode(ClassificationItem(None, -1, false, true,  false, "2013-01-01", 7, "B29C31/00",  title7, notes2), Seq(level8))
      
      val title6 = """<class-title date-revised="2013-01-01">
                <title-part>
                    <text scheme="ipc">faking text for title6</text>
                </title-part>
            </class-title>"""
      val level6 = TreeNode(ClassificationItem(None, -1, false, true,  false, "2013-01-01", 6, "B29C31/00",  title6, notes2), Seq(level7))
      
      val title5 = """<class-title date-revised="2013-01-01">
                <title-part>
                    <text scheme="ipc">faking text for title5v</text>
                </title-part>
            </class-title>"""
      val level5 = TreeNode(ClassificationItem(None, -1, false, false, false, "2013-01-01", 5, "B29C",       title5, notes2), Seq(level6))
      
      indexer.addTree(level5)
      }
    
    dir
  }
}