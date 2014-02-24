# t3as - Text analysis as a service

## Patent Classification web services

This project contains two web services for doing Patent Classification Search and Lookups of the CPC, IPC, and USPC patent classification systems.

Source code is released as GPL open source and JSON web services are publicly accessible at <http://pat-clas.t3as.org/>.

# Services

The following services are provided:

- Convert CPC/IPC/USPTO code to list of string descriptions, one for the code itself and one for each ancestor in the hierarchy. A very simple database app with XML processing to populate the database.

- Given a text query, find CPC/IPC/USPTO codes that have descriptions matching the query. A very simple Lucene search app.

# Source code structure

These services are implemented in a Maven multi-module Scala project.

- t3as-pat-clas the Maven parent project (no code)
- pat-clas-api:  Java API for the services: patent classification lookup and search.
  A factory provides dynamic loading of the implementation (in-process or remote) with no client code change required to switch.
- pat-clas-common:  Shared classes for following projects
- pat-clas-db:  Slick database layer to create and populate the database and lookup patent classifications.

Implementations of the patent classification lookup service:

- pat-clas-lookup:  in-process implementation of the lookup service. Configuration in lookup.properties.
- pat-clas-lookup-web:  JSON web service to expose the above
- pat-clas-lookup-client:  remote implementation of the lookup service using the JSON web service. Configuration in lookup-client.properties.

Implementations of the patent classification search service:

- pat-clas-search:  in-process implementation of the search service. Configuration in search.properties.
- pat-clas-search-web:  JSON web service to expose the above
- pat-clas-search-client:  remote implementation of the search service using the JSON web service. Configuration in search-client.properties.

Parser + database loader + search indexer:

- pat-clas-parse:  Parse the classification definition files and populate the database and search indices.

Example user interface:

- pat-clas-ui:  A web page using jQuery to access the JSON web services. See [pat-clas-ui](pat-clas-ui).
 
# Building

Just run maven from the top level directory:

	mvn

# Configuring

The webapps are configured using properties files in the source tree at `src/main/resources`, which are copied to `WEB-INF/classes`
in the webapp. The webapps will preferentially take values from an environment variable with the same name as a property.
If you use paths other than defaults used in the parser example below for the location of the database and
search indices, you'll need to either set environment variables or edit these property files before running the webapps.

# Running

## Downloading Data

From the data directory, run `download.sh` to:

 - download classification source data;
 - download and install the `fancytree` jQuery widget into `pat-clas-ui`;
 - create an empty US DTD file; and
 - patch corrupt data for US class 560.

## Creating Deployable Packages

Prerequisites are Building and Downloading Data, then from the data directory, run `package.sh` to:

 - parse the data to create the database and search indices (further details provided in the following section)
 - create tar.gz archives for the dynamic web apps `pat-class-lookup-web` and `pat-clas-search-web`, which
   contain the war file and the database or search indices it depends on
 - create a tar.gz archive for `pat-clas-ui`, which contains static files packed inside a .war file just so that
   it can be deployed using the same mechanism as the dynamic web apps.

## Parsing

Note: This step is automated by `package.sh` described above.

From the data directory, run the parser to create the database and search indices like this:

	java -jar ../pat-clas-parse/target/pat-clas-parse-1.0.one-jar.jar
		
(use --help option for further details).

Notes:

 - To omit CPC processing provide a non-existent path for the CPC zip file, e.g. <code>--cpcZipFile none</code> and likewise for IPC and USPC.
 - (*) XML in <code>ipcr\_scheme\_20130101.zip</code> refers to its DTD with <code>&lt;!DOCTYPE revisionPeriods SYSTEM "ipcr\_scheme\_1-02.dtd"&gt;</code>
   so IPC processing requires this file in the current working directory. We could add an entity resolver to pick this
   up from a jar file class path entry, but I don't think its worth doing.
 - (*) XML in <code>classdefs.zip</code> refers to its DTD with <code>&lt;!DOCTYPE class PUBLIC "-//USPTO//DTD Classification Definitions//EN" "xclassdef.dtd"&gt;</code>
   so USPC processing requires this file in the current working directory. As the provided DTD is not valid, just create an empty file with this name.
   At some stage we may want to create a valid DTD containing at least the entity definitions from the invalid provided DTD.
 - The suggested actions for the items marked (*) above are automated by the <code>data/download.sh</code> script.
   
You can run a database server and open a SQL GUI in your web browser with:

		java -jar ~/.m2/repository/com/h2database/h2/1.3.173/h2-1.3.173.jar
		
(in the GUI enter the dburl that was used with the parser and blank user name and password). Only one process can
have the database open at a time, so stop this server (with Ctrl-C) before starting the <code>pat-clas-lookup-web</code> web app.

## Running JSON Web Services

In a <code>*-web</code> project use:

		mvn tomcat7:run
		
to build and deploy the webapp on tomcat on port 8080. Use Ctrl-C to stop tomcat.

To run outside of Maven, copy the target/*.war files to tomcat's webapps dir or otherwise install these war files in the
Servlet 3.0 compliant app server of your choice. 

# Using Services

## Example user interface

The example user interface [pat-clas-ui](pat-clas-ui) demonstrates AJAX access to the JSON Web Services.

## Accessing the web services

The following sections show how to access:

 - remote services using HTTP requests; and
 - local (in-process) or remote services using the Java API.

## Lookup

Lookup a symbol in the database and fetch the <code>&lt;class-title&gt;</code> and <code>&lt;notes-and-warnings&gt;</code> of it and its ancestors:

 1. In a browser:
<http://localhost:8080/pat-clas-lookup-web/v1.0/CPC/A01B?f=xml>
; or

 2. on the command line:
<code>curl http://localhost:8080/pat-clas-lookup-web/v1.0/CPC/A01B?f=xml | python -mjson.tool</code>
; or

 3. in Java (see LookupMain.java in the pat-clas-examples project for the full example):

		Lookup<CPC.Description> lookup = Factories.getCPCLookupFactory().create();
		List<CPC.Description> xmlSnippets = lookup.getAncestors("H05K2203/0743", Format.XML);
		log.info("CPC xmlSnippets = {}", xmlSnippets);

With <code>?f=xml</code> (f for format) the JSON response fields <code>classTitle</code> and <code>notesAndWarnings</code> contain XML snippets as they appear in the downloaded
CPC definitions (see item 4 in <a href="#CPCdef">CPC Scheme Definitions</a>).
A client may wish to present this to the user via a suitable XSLT transform;
embed it in HTML with suitable CCS definitions for display;
or parse the &lt;class-ref&gt; elements.
Without this option these fields contain only the text from the CPC definitions, with tags removed.

To fetch the direct children of a symbol use the parent's unique id from a prior lookup e.g.:
<http://localhost:8080/pat-clas-lookup-web/v1.0/CPC/id/15/*?f=xml>

Similarly lookup IPC and USPC symbols by substituting these strings for CPC above.


## Search

Search the text associated with classification symbols:

 1. In a browser:
<http://localhost:8080/pat-clas-search-web/v1.0/CPC?q=attention>
; or

 2. on the command line:
<code>curl http://localhost:8080/pat-clas-search-web/v1.0/CPC?q=attention | python -mjson.tool</code>
; or

 3. in Java (see SearchMain.java in the pat-clas-examples project for the full example):

		Search<CPC.Hit> search = Factories.getCPCSearchFactory().create();
		List<CPC.Hit> hits = search.search("attention");
		log.info("CPC hits = {}", hits);

Similarly search IPC and USPC symbols by substituting these strings for CPC above. 
For details on the syntax of queries and queryable fields refer to [pat-clas-ui](pat-clas-ui).

# Deployment Choices

The choice of deploying a service in the same process as the client using it or accessing the service remotely, is made by
choosing which dependency jar file is provided to the client (with no code change).

Java (or Scala or other JVM based) clients of the lookup service have:

 - a compile time dependency on pat-clas-api;
 - for an in-process instance of the service they have a runtime dependency on pat-clas-lookup; or
 - to access the remote JSON web service (provided by pat-clas-lookup-web running elsewhere) they have a runtime dependency on pat-clas-lookup-client.

Likewise for the search service.

Non JVM based clients use HTTP to interact with the remote JSON web service.

# CPC

## CPC Structure

There are 5 levels in the structure of a CPC symbol:

 1. section - a letter  e.g. A
 2. class - two digits  e.g. A01
 3. subclass - a letter  e.g. A01B
 4. group - 1 to 3 digits  e.g. A01B3
 5. subgroup - at least two digits following a "/" ("00" for maingroup i.e. no subgroup)  e.g. A01B3/421

A CPC browser is available at: <http://worldwide.espacenet.com/classification/>

<a name="CPCdef"/>
## CPC Scheme Definitions

The complete CPC scheme, defined in XML, is available at: 
<http://www.cooperativepatentclassification.org/cpcSchemeAndDefinitions/Bulk.html>.
The current version is at the link: <a href="http://www.cooperativepatentclassification.org/cpc/CPCSchemeXML201309.zip">Complete CPC scheme in XML format (2013-09)</a>.

CPC zip file contents:

 1. <code>&lt;classification-item&gt;</code>s in the XML have a level attribute with values from 2 to 16.
 1. The associations appear to be: level 2 -> section; 3 & 4 -> class; 5 -> subclass; 6 & 7 -> group; > 8 -> subgroup.
 1. Where multiple level numbers map to the same level in the CPC hierarchy (e.g. 3 & 4 -> class) then the text associated with all the level numbers
   must be used to obtain the full description (e.g. text from level 3 followed by text from level 4).
 4. The descendant elements and text in <code>&lt;class-title&gt;</code> and <code>&lt;notes-and-warnings&gt;</code> elements appears intended for XSLT transformation to HTML
   for presentation to the user. Features leading to this conclusion are: 1) the use of <code>&lt;pre&gt;</code> and <code>&lt;br&gt;</code> tags;
   and 2) the use of alternating pairs of text referring to another classification and a <code>&lt;class-ref&gt;</code> tag specifying the symbol referred to
   (relying on the ordering of siblings).
 1. scheme.xml defines all top level sections but with no descriptive text.
   Each item refers to the files below e.g. with link-file="cpc-A.xml" which refers to the actual file scheme-A.xml,
   which defines the detailed content of the section.
 1. scheme-X.xml defines section X in detail from levels 2 to 5.
   The level 5 items refer to the files below e.g. link-file="cpc-A01B.xml" refers to the actual file scheme-A01B.xml.
 1. scheme-XnnY defines subclass XnnY in detail from levels 5 to 16.
   The level 5 content is identical in these two files, so only one should be loaded.

# IPC

## IPC Structure

IPC uses the same 5 levels as CPC and is mostly a subset of CPC (CPC is based on IPC).
IPC has fewer top level classes and has less detail at the subgroup (lowest) level.

An IPC browser is available at: <http://web2.wipo.int/ipcpub/>

<a name="IPCdef"/>
## IPC Scheme Definitions

IPC documentation is available at:
<http://www.wipo.int/classifications/ipc/en/>.

Following the links <a href="http://www.wipo.int/classifications/ipc/en/support/">Download and IT Support</a>
then
<a href="http://www.wipo.int/classifications/ipc/en/ITsupport/Version20130101/index.html">Access current edition: 2013.01</a>
then
MASTER FILES <a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/20130101/MasterFiles/">Download</a>
gets to the zipped XML definitions:
<a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/20130101/MasterFiles/ipcr_scheme_20130101.zip">ipcr\_scheme\_20130101.zip</a>.

The DTD is available at:
<a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/Documentation/20140101/ipcr_scheme_1-02.dtd">ipcr\_scheme_1-02.dtd</a>.

# U.S. Classifications

The USPTO refer to their legacy classifications as the U.S. Patent Classification (USPC) System. This is being phased out in favour of the CPC.

## USPC Structure

USPC symbols have a 2 level structure:

 1. class - up to 3 digits, or
    D followed by up to 2 digits (Design Patents), or
    PLT (Plant Patents); and
 2. subclass - two sets of up to 3 digits separated by a '.'. Either set of digits may be omitted for a zero value.
    The '.' is required if the 2nd set is present, optional if not. The second set appears to be used to insert
    new entries in between relevant previously existing entries.
 3. In UI's the number of leading dots in the description is used to indicate nesting level.
 
A USPC browser is available at: <http://www.uspto.gov/web/patents/classification/>
 
The nesting of subclasses is not reflected in the numbering, for example, in Class 73 "MEASURING AND TESTING":
 
  - top level subclasses include: 1.01 "INSTRUMENT PROVING OR CALIBRATING"; and 7 "BY ABRASION, MILLING, RUBBING, OR SCUFFING"
  - subclasses at different levels include: 1.72 "...Valve"; 1.73 ".Liquid level or volume measuring apparatus"; and 1.74 "..Volumetric dispenser (e.g., pipette)" 

<a name="USPCdef"/>
## USPC Scheme Definitions

The USPC scheme, defined in what purports to be XML, is available from: 
<http://www.uspto.gov/products/catalog/additionalpatentdata.jsp#heading-8> by following the links
<https://eipweb.uspto.gov/2013/ClassDefinitions/> and then
<a href="https://eipweb.uspto.gov/2013/ClassDefinitions/classdefs.zip">classdefs.zip</a>.

### DTD

The XML definitions refer to a DTD with:
		
		<!DOCTYPE class PUBLIC "-//USPTO//DTD Classification Definitions//EN" "xclassdef.dtd">

The zip file contains <code>classdef.dtd</code> which would appear to be a candidate for this DTD.

### Graphics

As well as the XML definitions, the zip file contains small TIFF images illustrating the classifications.
In many domains, such as chemistry, mechanics and industrial design, these could be useful to quickly identify relevant classifications.
XML zip file entries, e.g. <code>classdefs201308/class\_106.xml</code>, refer to these illustrations with the attribute
<code>subclass/notes/note1/figure/graphic/@filename</code>. The attribute value is upper case,
whereas the zip file entries for the TIFF images have lower case names, e.g. <code>filename="C106S124-1A.TIF"</code> refers to
the zip entry <code>classdefs201308/c106s124-1a.tif</code>.

### Subclass Nesting

Nesting of elements is not used to represent subclass nesting and all subclasses are sibling elements. Instead a <code>subclass/parent/@ref</code>
attribute refers to its parent <code>class|subclass/@id</code>. E.g.
		
		<subclass subnum="1" id="C002S001000">
		  <sctitle>MISCELLANEOUS:</sctitle>
		  <parent ref="C002S000000" doc="Class002.sgm">the class definition</parent>

refers to
		
		<class classnum="2" id="C002S000000">
		
### Data Issues

 - The provided DTD <code>classdef.dtd</code> does not parse as a valid XML DTD.
 
 - Because we don't use a DTD the stored XML snippets do not have entities expanded (making it harder for
   anyone using this data). We could create a valid DTD, at least to define the entities.
 
 - The <code>&lt;graphic&gt;</code> tag is not closed so the zip entries containing it are not well formed XML.
 
 - The <code>subclass/parent</code> element is missing in 1123 cases, e.g. in zip entry <code>classdefs201308/class\_104.xml</code>:

		<subclass subnum="117" id="C104S117000">
		  <sctitle>Tension regulators and anchors:</sctitle>
		  <scdesc>Devices under subclasses 112 comprising cable-rail tension regulators and anchoring devices for the ends of cable-rails.</scdesc>
		</subclass>

   It appears from the description that subclass 112 should be the parent. Nothing in subclass 112 indicates that subclass 117
   should be its child. I'm assigning a dummy parent "none" until we have something better in place.
   We could parse the description to hazard a better guess, but scraping the USPTO classification browser would probably be best.
   
 - There are 32 cases of the invalid subclass <code>subclass/@subnum="-2"</code>
   e.g. in zip entry <code>classdefs201308/class_14.xml</code>.
   A rule that derives the subclass from <code>subclass/@id</code> in such cases recovers a small number of useful records.
   Out of 5 cases where this rule was invoked (see the next section for 27 cases where it was not)
   it produced useful data in 3 cases (id = C338S218000, C379S106020, C600S231000),
   one record that is valid, harmless, but contains no useful information (id = C568S037000),
   and one useless record that was discarded as having a duplicate key with the correct record (id = C475S075000). 
   
 - In 27 of the 32 cases with invalid subclass, the <code>subclass/@id</code> is also invalid e.g.
 
		<subclass subnum = "-2" id = "C123S028&ast;&ast;2">
		  <sctitle>Oil-Engines:</sctitle>
		  
  Each of these cases has an asterisk in the id. In such cases we'll log an error and skip the subclass.
  I have yet to check whether the skipped data is covered elsewhere.
  
 - zip entry <code>classdefs201308/class\_261.xml</code> contains two instances of
   the same supposedly unique value <code>subclass/@id = C261S021000</code>.
   The second is clearly in error and should be <code>C261S022000</code>:

		<subclass subnum="22" id="C261S021000">

  To correct such errors we generate the id from the classnum and subnum. When it differs from the provided id we log
  the values and use the generated value. This is invoked in 8 cases.
  
 - There are 3 cases of duplicate id's (there were 8 prior to implementing the rule above):
   1 case was discussed above in the discussion of <code>subclass/@subnum="-2"</code> : id = C475S075000.
   In the remaining 2 cases the correct records appear twice, so discarding the duplicates is correct behaviour:
   id = C604S006060, CD09S716000.
  
 - zip entry <code>classdefs201308/class\_560.xml</code> is missing the class definition. It starts with:
 
		>
		<sclasses>
		  <subclass subnum = "1" id = "C560S001000">

  It also has the <code>&lt;sclasses&gt;</code> element and all the subclasses duplicated. This can only be addressed by manual correction of the entry using another data source.

