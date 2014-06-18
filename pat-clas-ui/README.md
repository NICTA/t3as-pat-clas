t3as
====

Patent Classification Schemes - User Interface
----------------------------------------------

### Introduction

The web page [index.html](index.html) provides a user interface to the JSON web services provided by
`pat-clas-service`.

It requires the jQuery-ui plugin "Fancytree" in the dir `fancytree/` (under same dir as `index.html`).
Installation of fancytree is one of the actions automated by `data/download.sh`;
or `jquery.fancytree-2.0.0-4.zip` can be manually downloaded from: <http://plugins.jquery.com/fancytree/>.

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
   
#### Selection Action

   Select `Context` to update the Context section whenever a classification symbol is clicked, or `Explore` to update the Explore section. 
	
#### Search
 
   Results for the full-text query are displayed including snippets of matching text, limited to the top 50 hits.
   Clicking on a classification symbol displays it in either the Context or Explore sections. 
   The query syntax is described at:
   <http://lucene.apache.org/core/4_8_1/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description>
   and the fields available for use in queries are:
   <dl>
     <dt>CPC</dt><dd>Symbol, Level, **ClassTitle**, **NotesAndWarnings**, ClassTitleUnstemmed, NotesAndWarningsUnstemmed;</dd>
     <dt>IPC</dt><dd>Symbol, Level, Kind, **TextBody**, TextBodyUnstemmed;</dd>
     <dt>USPC</dt><dd>Symbol, **ClassTitle**, **SubClassTitle**, **SubClassDescription**, **Text** (all text not included in the previous fields),
     ClassTitleUnstemmed, SubClassTitleUnstemmed, SubClassDescriptionUnstemmed, TextUnstemmed;</dd>
   </dl>
   The fields shown in bold:
   
 - are searched by default when the query does not specify any fields; and
 - have stemming applied (a search for `activated` will match `activating` and vice-versa).
 
 
 **Note:** If `stem` is set to false then the corresponding `Unstemmed` fields are searched by default.
   
#### Context
 
   The selected symbol is shown along with all its ancestors, allowing the hierarchical text to be read in its proper context.
   References to symbols are shown as links which re-populate either the Context or Explore section:
   <dl>
     <dt>CPC</dt><dd>links to other CPC symbols are blue, links to IPC symbols are red.
     Selecting an IPC link switches the schema in the Schema Selection section.</dd>
     <dt>IPC</dt><dd>links to IPC symbols are blue</dd>
     <dt>USPC</dt><dd>links to fully specified symbols are blue, links specifying the just the subclass (class given by context) are green,
     unless a range is referred to, in which case the starting subclass is green and the ending subclass is red</dd>
   </dl>

#### Explore
 
   Initially the specified symbol and all its ancestors are
   shown (as in the Context section - i.e. each node is shown with only a single child).
   However, clicking on any node causes all of its children to be displayed, allowing the full tree to be explored.
   Clicking on a symbol link in the tree re-populates either the Context or Explore section.
   The tree omits the CPC NotesAndWarnings and USPC Text fields in order to keep the size of the displayed text manageable.
   Use the Context section to view this information. The tree includes these fields:
   <dl>
     <dt>CPC</dt><dd>Symbol, Level, ClassTitle (not NotesAndWarnings)</dd>
     <dt>IPC</dt><dd>Symbol, Level, Kind, TextBody</dd>
     <dt>USPC</dt><dd>Symbol, ClassTitle, SubClassTitle, SubClassDescription (not Text)</dd>
   </dl>
 
   
   

