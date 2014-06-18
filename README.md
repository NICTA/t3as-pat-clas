# t3as - Text analysis as a service

## Patent Classification web services

This project provides web services for doing search and lookups of the CPC, IPC, and USPC patent classification systems.

Resources:

- [blog post](http://t3as.wordpress.com/2014/02/10/text-analytics-for-patent-classification/) for general information
- GPL source code (and this documentation) <https://github.com/NICTA/t3as-pat-clas>
- user interface and JSON web services <http://pat-clas.t3as.org/>
- user interface GPL source code and detailed documentation <https://github.com/NICTA/t3as-pat-clas/tree/master/pat-clas-ui>

# Services

The following JSON web services are provided:

- Convert CPC/IPC/USPTO symbol to list of string descriptions, one for the symbol itself and one for each ancestor in the hierarchy.

- Given a text query, find CPC/IPC/USPTO codes that have descriptions matching the query.

# Source code structure

These services are implemented in a Maven multi-module Scala project.

- **t3as-pat-clas**:  Maven parent project (no code)
- **pat-clas-api**:  Java and Scala API for the services: patent classification lookup and search
- **pat-clas-common**:  Shared classes for following projects
- **pat-clas-parse**:  Parse the classification definition files and populate the database and search indices
- **pat-clas-service**:  Implementation of the services. Client access can be in-process or remote using the Java or Scala API or remote using JSON HTTP requests from other languages (see [pat-clas-ui](pat-clas-ui) for an example of remote access from Javascript). Configuration in patClasService.properties.
- **pat-clas-client**:  Java and Scala client implementating the same API as pat-clas-service, but by issuing web service calls the services which must be running elsewhere.
- **pat-clas-ui**:  A web page using jQuery and AJAX to access the JSON web services. See [pat-clas-ui](pat-clas-ui).
 
# Building

Just run maven from the top level directory:

	mvn

# Configuring

The webapp is configured using a properties files in the source tree at `pat-clas-service/src/main/resources`, which is copied to `WEB-INF/classes`
in the webapp. The webapps will preferentially take values from a system property with the same name.
If you use paths other than defaults used in the parser example below for the location of the database and
search indices, you'll need to either set system properties or edit this property files before running the webapp.

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
 - create a tar.gz archive for the dynamic web app `pat-class-service`, which
   contains the war file and the database and search indices it depends on
 - create a tar.gz archive for `pat-clas-ui`, which contains static files packed inside a .war file just so that
   it can be deployed using the same mechanism as the dynamic web app.

## Parsing

Note: This step is automated by `package.sh` described above.

From the data directory, run the parser to create the database and search indices like this:

	java -jar ../pat-clas-parse/target/pat-clas-parse-1.0.one-jar.jar
		
(use --help option for further details).

Notes:

 - To omit CPC processing provide a non-existent path for the CPC zip file, e.g. `--cpcZipFile none` and likewise for IPC and USPC.
 - (*) XML in `ipcr_scheme_20130101.zip` refers to its DTD with `<!DOCTYPE revisionPeriods SYSTEM "ipcr_scheme_1-02.dtd">`
   so IPC processing requires this file in the current working directory. We could add an entity resolver to pick this
   up from a jar file class path entry, but I don't think its worth doing.
 - (*) XML in `classdefs.zip` refers to its DTD with `<!DOCTYPE class PUBLIC "-//USPTO//DTD Classification Definitions//EN" "xclassdef.dtd">`
   so USPC processing requires this file in the current working directory. As the provided DTD is not valid, just create an empty file with this name.
   At some stage we may want to create a valid DTD containing at least the entity definitions from the invalid provided DTD.
 - The suggested actions for the items marked (*) above are automated by the `data/download.sh` script.
   
You can run a database server and open a SQL GUI in your web browser with:

		java -jar ~/.m2/repository/com/h2database/h2/1.3.173/h2-1.3.176.jar
		
(in the GUI enter the dburl that was used with the parser, READONLY for the user name and a blank password). Only one process can
have the database open at a time, so stop this server (with Ctrl-C) before starting the `pat-clas-lookup-web` web app.

## Running JSON Web Services

In `pat-clas-service` use:

		mvn tomcat7:run
		
to build and deploy the webapp on tomcat on port 8080. Use Ctrl-C to stop tomcat.

To run outside of Maven, copy the target/*.war files to tomcat's webapps dir or otherwise install these war files in the
Servlet 3.0 compliant app server of your choice. 

# Using Services

You can access the services from anything capable of issuing HTTP requests:

- the example user interface <http://pat-clas.t3as.org/> demonstrates access via jQuery AJAX
- web browser:
<http://pat-clas.t3as.org/pat-clas-service/rest/v1.0/CPC/search?q=locomotive&stem=false&symbolPrefix=F2>
- command line:
 `curl http://pat-clas.t3as.org/pat-clas-service/rest/v1.0/CPC/children?parentId=0&format=XML | python -mjson.tool`
- Java API: <https://github.com/NICTA/t3as-pat-clas/blob/refactor/pat-clas-examples/src/main/java/org/t3as/patClas/examples/javaApi/JavaExample.java>
- Scala API: <https://github.com/NICTA/t3as-pat-clas/blob/refactor/pat-clas-examples/src/main/scala/org/t3as/patClas/examples/scala/ScalaExample.scala>

## URL formats

The service URL's and query parameters are defined by the `@Path` and `@QueryParam` annotations in <https://github.com/NICTA/t3as-pat-clas/blob/refactor/pat-clas-service/src/main/scala/org/t3as/patClas/service/PatClasService.scala>.

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
The current version is at the link: <a href="http://www.cooperativepatentclassification.org/cpc/CPCSchemeXML201312.zip">Complete CPC scheme in XML format (2013-12)</a>.

CPC zip file contents:

 1. `<classification-item>`s in the XML have a level attribute with values from 2 to 16.
 1. The associations appear to be: level 2 -> section; 3 & 4 -> class; 5 -> subclass; 6 & 7 -> group; > 8 -> subgroup.
 1. Where multiple level numbers map to the same level in the CPC hierarchy (e.g. 3 & 4 -> class) then the text associated with all the level numbers
   must be used to obtain the full description (e.g. text from level 3 followed by text from level 4).
 4. The descendant elements and text in `<class-title>` and `<notes-and-warnings>` elements appears intended for XSLT transformation to HTML
   for presentation to the user. Features leading to this conclusion are: 1) the use of `<pre>` and `<br>` tags;
   and 2) the use of alternating pairs of text referring to another classification and a `<class-ref>` tag specifying the symbol referred to
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
<a href="http://www.wipo.int/classifications/ipc/en/ITsupport/Version20140101/index.html">Access current edition: 2014.01</a>
then
MASTER FILES <a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/20140101/MasterFiles">Download</a>
gets to the zipped XML definitions:
<a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/20140101/MasterFiles/ipcr_scheme_20140101.zip">ipcr_scheme_20140101.zip</a>.

The DTD is available at:
<a href="http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/Documentation/20140101/ipcr_scheme_1-02.dtd">ipcr_scheme_1-02.dtd</a>.

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
<a href="https://eipweb.uspto.gov/2013/ClassDefinitions/classdefs.zip">classdefs.zip</a> (last updated 2014-01-22).

### DTD

The XML definitions refer to a DTD with:
		
		<!DOCTYPE class PUBLIC "-//USPTO//DTD Classification Definitions//EN" "xclassdef.dtd">

The zip file contains `classdef.dtd` which would appear to be a candidate for this DTD.

### Graphics

As well as the XML definitions, the zip file contains small TIFF images illustrating the classifications.
In many domains, such as chemistry, mechanics and industrial design, these could be useful to quickly identify relevant classifications.
XML zip file entries, e.g. `classdefs201308/class_106.xml`, refer to these illustrations with the attribute
`subclass/notes/note1/figure/graphic/@filename`. The attribute value is upper case,
whereas the zip file entries for the TIFF images have lower case names, e.g. `filename="C106S124-1A.TIF"` refers to
the zip entry `classdefs201308/c106s124-1a.tif`.

### Subclass Nesting

Nesting of elements is not used to represent subclass nesting and all subclasses are sibling elements. Instead a `subclass/parent/@ref`
attribute refers to its parent `class|subclass/@id`. E.g.
		
		<subclass subnum="1" id="C002S001000">
		  <sctitle>MISCELLANEOUS:</sctitle>
		  <parent ref="C002S000000" doc="Class002.sgm">the class definition</parent>

refers to
		
		<class classnum="2" id="C002S000000">
		
### Data Issues

 - The provided DTD `classdef.dtd` does not parse as a valid XML DTD.
 
 - Because we don't use a DTD the stored XML snippets do not have entities expanded (making it harder for
   anyone using this data). We could create a valid DTD, at least to define the entities.
 
 - The `<graphic>` tag is not closed so the zip entries containing it are not well formed XML.
 
 - The `subclass/parent` element is missing in 1123 cases, e.g. in zip entry `classdefs201308/class_104.xml`:

		<subclass subnum="117" id="C104S117000">
		  <sctitle>Tension regulators and anchors:</sctitle>
		  <scdesc>Devices under subclasses 112 comprising cable-rail tension regulators and anchoring devices for the ends of cable-rails.</scdesc>
		</subclass>

   It appears from the description that subclass 112 should be the parent. Nothing in subclass 112 indicates that subclass 117
   should be its child. I'm assigning a dummy parent "none" until we have something better in place.
   We could parse the description to hazard a better guess, but scraping the USPTO classification browser would probably be best.
   
 - There are 32 cases of the invalid subclass `subclass/@subnum="-2"`
   e.g. in zip entry `classdefs201308/class_14.xml`.
   A rule that derives the subclass from `subclass/@id` in such cases recovers a small number of useful records.
   Out of 5 cases where this rule was invoked (see the next section for 27 cases where it was not)
   it produced useful data in 3 cases (id = C338S218000, C379S106020, C600S231000),
   one record that is valid, harmless, but contains no useful information (id = C568S037000),
   and one useless record that was discarded as having a duplicate key with the correct record (id = C475S075000). 
   
 - In 27 of the 32 cases with invalid subclass, the `subclass/@id` is also invalid e.g.
 
		<subclass subnum = "-2" id = "C123S028&ast;&ast;2">
		  <sctitle>Oil-Engines:</sctitle>
		  
  Each of these cases has an asterisk in the id. In such cases we'll log an error and skip the subclass.
  I have yet to check whether the skipped data is covered elsewhere.
  
 - zip entry `classdefs201308/class_261.xml` contains two instances of
   the same supposedly unique value `subclass/@id = C261S021000`.
   The second is clearly in error and should be `C261S022000`:

		<subclass subnum="22" id="C261S021000">

  To correct such errors we generate the id from the classnum and subnum. When it differs from the provided id we log
  the values and use the generated value. This is invoked in 8 cases.
  
 - There are 3 cases of duplicate id's (there were 8 prior to implementing the rule above):
   1 case was discussed above in the discussion of `subclass/@subnum="-2"` : id = C475S075000.
   In the remaining 2 cases the correct records appear twice, so discarding the duplicates is correct behaviour:
   id = C604S006060, CD09S716000.
  
 - zip entry `classdefs201308/class_560.xml` is missing the class definition. It starts with:
 
		>
		<sclasses>
		  <subclass subnum = "1" id = "C560S001000">

  It also has the `<sclasses>` element and all the subclasses duplicated. This can only be addressed by manual correction of the entry using another data source.

