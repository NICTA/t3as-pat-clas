#! /bin/bash

# # Steps to release and deploy a new version to EC2
# # ================================================

# # update poms for release
# mvn versions:set -DnewVersion=1.0.3
# mvn versions:commit
# git commit -a -m 'update version for release'
# git push
# 
# # create a release tag
# git tag -a pat-clas-1.0.3 -m 'tag for release'
# git push origin pat-clas-1.0.3
# 
# # do a clean build
# mvn clean
# mvn
# 
# # create packages to deploy on ec2
# cd data
# # optionally comment out running pat-clas-parse-*.one-jar.jar near the top if there is no data change
# ./package.sh
# 
# # upload packages to s3
# Log in to EC2, S3 bucket t3as-webapps
# upload the 3 *.tar.gz files created by package.sh
# 
# Do Mats' magic deplopyment stuff.
# 
# # update poms for ongoing dev
# mvn versions:set -DnewVersion=1.0.4-SNAPSHOT
# mvn versions:commit
# git commit -a -m 'update version for dev'

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



