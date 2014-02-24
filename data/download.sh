#! /bin/sh

set -e

D_DIR=`pwd`

# Download CPC, IPC and USPC patent classification schemes.
# While we're at it, also get the jquery-ui-fancytree widget required by pat-clas-ui.
while read url
do
    echo "Downloading $url..."
    wget --no-clobber $url
done <<'EoF'
http://www.cooperativepatentclassification.org/cpc/CPCSchemeXML201309.zip
http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/20130101/MasterFiles/ipcr_scheme_20130101.zip
http://www.wipo.int/ipc/itos4ipc/ITSupport_and_download_area/Documentation/20140101/ipcr_scheme_1-02.dtd
https://eipweb.uspto.gov/2013/ClassDefinitions/classdefs.zip
https://github.com/mar10/fancytree/releases/download/v2.0.0-4/jquery.fancytree-2.0.0-4.zip
EoF

# The USPC zip file contains a DTD "classdef.dtd" but it is not a valid XML DTD (presumably SGML?)
# The USPC data refers to a DTD "xclassdef.dtd", so this file must exist.
# Until we convert the DTD to a valid XML DTD, we use an empty file.
touch xclassdef.dtd

# The USPC zip file contains a corrupted entry for class 560.
# This reproduces a manual edit to produce a fixed version classdefsWith560fixed.zip
unzip classdefs.zip classdefs201312/class_560.xml
patch classdefs201312/class_560.xml class_560.xml.diff
cp classdefs.zip classdefsWith560fixed.zip
zip -f classdefsWith560fixed.zip classdefs201312/class_560.xml

# Install jquery-ui-fancytree to where pat-clas-ui expects it
FT_DIR=../pat-clas-ui/fancytree
mkdir -p $FT_DIR
( cd $FT_DIR; unzip $D_DIR/jquery.fancytree-2.0.0-4.zip )

