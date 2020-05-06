#!/usr/bin/env bash
version="1.0.3-SNAPSHOT"
for file in $(find \./Z-* -name "pom.xml");do
     echo $file
     sed -e "33s/<version>\(.*\)<\/version>/<version>${version}<\/version>/g$h" \
     $file > $file.new
     mv -f $file.new $file
done
sed -e "33s/<version>\(.*\)<\/version>/<version>${version}<\/version>/g$h" ./pom.xml > ./pom.xml.new
mv -f ./pom.xml.new ./pom.xml