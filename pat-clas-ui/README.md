t3as
====

Patent Classification Schemes - User Interface
----------------------------------------------

### Introduction

The web page <index.html> provides a user interface to the JSON web services provided by
<code>pat-clas-lookup-web</code> and <code>pat-clas-search-web</code>.

It uses jQuery's support for [JSONP](http://en.wikipedia.org/wiki/JSONP) to work around the browser's
[Same Origin Policy](http://en.wikipedia.org/wiki/Same-origin_policy) in order to support cross site mashups.
Consequently you can save the web page locally, modify it and still access the remotely hosted t3as web services.

It requires the jQuery-ui plugin "Fancytree" in the dir <code>fancytree/</code> (under same dir as <code>index.html</code>).
Installation of fancytree is one of the actions automated by <code>data/download.sh</code>;
or <code>jquery.fancytree-2.0.0-4.zip</code> can be manually downloaded from: <http://plugins.jquery.com/fancytree/>.

### Description

The UI consists of the following sections:

#### Schema Selection
 
   A patent classification schema is selected using one of the CPC, IPC or USPC Radio buttons.
   A link to the official browser for the selected schema is provided.
   There are unfortunately many cases where our input data quality is lower than that of the official browser
   and this is most apparent with the USPC.
   In most cases the errors are readily apparent (e.g. 1123 US subclasses have no parent specified,
   so they are shown at the top of the hierarchy; or invalid references to other classes causing a broken link).
   In such cases the interested user is encouraged to consult the official browser.
   
#### Search
 
   Results for the full-text query are displayed including snippets of matching text, limited to the top 50 hits.
   Clicking on a classification symbol displays it in the Context section. 
   The query syntax is that of: [org.apache.lucene.queryparser.classic.QueryParser](http://lucene.apache.org/core/4_6_0/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html)
   and the fields available for use in queries are:
   <dl>
     <dt>CPC</dt><dd>Symbol, Level, <b>ClassTitle</b>, <b>NotesAndWarnings</b>;</dd>
     <dt>IPC</dt><dd>Symbol, Level, Kind, <b>TextBody</b>;</dd>
     <dt>USPC</dt><dd>Symbol, <b>ClassTitle</b>, <b>SubClassTitle</b>, <b>SubClassDescription</b>, <b>Text</b> (all text not included in the previous fields);</dd>
   </dl>
   The fields shown in bold:
   
 - are searched by default when the query does not specify any fields; and
 - have stemming applied (a search for <code>activated</code> will match <code>activating</code> and vice-versa).
   
#### Context
 
   The selected symbol is shown along with all its ancestors, allowing the hierarchical text to be read in its proper context.
   References to symbols are shown as links which re-populate the Context section:
   <dl>
     <dt>CPC</dt><dd>links to other CPC symbols are blue, links to IPC symbols are red.
     Selecting an IPC link switches the schema in the Schema Selection section.</dd>
     <dt>IPC</dt><dd>links to IPC symbols are blue</dd>
     <dt>USPC</dt><dd>links to fully specified symbols are blue, links specifying the just the subclass (class given by context) are green,
     unless a range is referred to, in which case the starting subclass is green and the ending subclass is red</dd>
   </dl>

#### Explore
 
   Paste a symbol from elsewhere to explore the classification system in a tree. Initially the specified symbol and all its ancestors are
   shown (as in the Context section - i.e. each node is shown with only a single child).
   However, clicking on any node causes all of its children to be displayed, allowing the full tree to be explored.
   Clicking on a symbol link in the tree re-populates the Context section.
   The tree omits the CPC NotesAndWarnings and USPC Text fields in order to keep the size of the displayed text manageable.
   Use the Context section to view this information. The tree includes these fields:
   <dl>
     <dt>CPC</dt><dd>Symbol, Level, ClassTitle (not NotesAndWarnings)</dd>
     <dt>IPC</dt><dd>Symbol, Level, Kind, TextBody</dd>
     <dt>USPC</dt><dd>Symbol, ClassTitle, SubClassTitle, SubClassDescription (not Text)</dd>
   </dl>
 
   
   

