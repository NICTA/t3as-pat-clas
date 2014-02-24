#! /bin/bash

set -e

java -jar ../pat-clas-parse/target/pat-clas-parse-*.one-jar.jar

rm -rf tmp

# package dynamic web apps as a .tar.gz containing the war file and the static data it depends on
for n in pat-clas-lookup-web pat-clas-search-web
do
    mkdir -p tmp/$n/data
    src=../$n/target/$n-*.war
    ver=`echo $src | sed -e 's/.*web-//' -e 's/.war//'`
    echo "Creating $n-$ver.tar.gz ..."
    cp $src tmp/$n/$n.war
    case "$n" in
        pat-clas-lookup-web) cp patClasDb.h2.db tmp/$n/data;;
        pat-clas-search-web) cp --recursive {cpc,ipc,uspc}Index tmp/$n/data
    esac
    tar cfz $n-$ver.tar.gz -C tmp $n
done

# the ui has no server component, but package it same as above to facilitate deployment
n=pat-clas-ui
echo "Creating $n-$ver.tar.gz ..." # reuse ver from above
mkdir -p tmp/$n 
d=`pwd`
cd ../$n
jar cf $d/tmp/$n/$n.war ajax-loader.gif  fancytree  index.css  index.html
cd $d
tar cfz $n-$ver.tar.gz -C tmp $n



